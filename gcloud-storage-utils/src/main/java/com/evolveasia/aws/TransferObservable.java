package com.evolveasia.aws;

import android.util.Log;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.evolveasia.initializer.ContextProviderImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import static com.evolveasia.aws.Util.getTransferUtility;


public class TransferObservable extends Observable<TransferUpdate> {
    public static final long FILE_SIZE_UNLIMITED = Long.MAX_VALUE;
    private final AwsMetaInfo awsMetaInfo;
    private final TransferUtility transferUtility;
    private final String filePath;
    private final String uploadPath;
    private final long maxSizeInBytes;

    public TransferObservable(AwsMetaInfo awsMetaInfo, String filePath, String uploadPath) {
        this(awsMetaInfo, filePath, uploadPath, FILE_SIZE_UNLIMITED);
    }

    public TransferObservable(AwsMetaInfo awsMetaInfo, String filePath, String uploadPath, long maxSizeInBytes) {
        this.awsMetaInfo = awsMetaInfo;
        this.transferUtility = getTransferUtility(awsMetaInfo);
        this.filePath = filePath;
        this.uploadPath = uploadPath;
        this.maxSizeInBytes = maxSizeInBytes;
    }


    @Override
    protected void subscribeActual(io.reactivex.Observer<? super TransferUpdate> observer) {
        File file = new File(filePath);
        if (!file.exists()) {
            observer.onError(new FileNotFoundException());
        } else if (maxSizeInBytes < file.length()) {
            observer.onError(new Exception("file size(" + file.length() + "bytes) is greater than max filesize(" + maxSizeInBytes + ")"));
        } else if (!Util.isNetworkAvailable(ContextProviderImpl.Companion.getInstance().getAppCtx())) {
            observer.onError(new Exception("Check your internet connection"));
        } else {
            final String fileName = file.getName();
            TransferObserver transferObserver = transferUtility.upload(awsMetaInfo.getServiceConfig().getBucketName(), uploadPath, file);
            Listener listener = new Listener(observer, transferObserver, fileName, uploadPath);
            observer.onSubscribe(listener);
            transferObserver.setTransferListener(listener);
        }
    }

    private static String buildAmazonS3Url(String uploadPath, String filename) {
        return uploadPath;
    }

    private static final class Listener implements TransferListener, Disposable {
        private final Observer<? super TransferUpdate> observer;
        private final TransferObserver transferObserver;
        private final String fileName;
        private final String uploadPath;
        private final AtomicBoolean unsubscribed = new AtomicBoolean();

        Listener(io.reactivex.Observer<? super TransferUpdate> observer, TransferObserver transferObserver, String fileName, String uploadPath) {
            this.observer = observer;
            this.transferObserver = transferObserver;
            this.fileName = fileName;
            this.uploadPath = uploadPath;
        }


        @Override
        public void onStateChanged(int id, TransferState state) {
            if (!isDisposed()) {
                if (state.equals(TransferState.COMPLETED)) {
                    observer.onNext(new TransferUpdate(TransferUpdate.STATE_COMPLETED, id, state, -1, -1, buildAmazonS3Url(uploadPath, fileName)));
                    observer.onComplete();
                } else if (state.equals(TransferState.FAILED)) {
                    observer.onError(new Exception("Transfer Failed"));
                }
            }
        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            if (!isDisposed())
                observer.onNext(new TransferUpdate(TransferUpdate.PROGRESSED_CHANGED, id, null, bytesCurrent, bytesTotal, null));
        }

        @Override
        public void onError(int id, Exception ex) {
            if (!isDisposed())
                observer.onError(ex);
        }

        @Override
        public void dispose() {
            if (unsubscribed.compareAndSet(false, true)) {
                transferObserver.cleanTransferListener();
            }
        }

        @Override
        public boolean isDisposed() {
            return unsubscribed.get();
        }
    }
}
