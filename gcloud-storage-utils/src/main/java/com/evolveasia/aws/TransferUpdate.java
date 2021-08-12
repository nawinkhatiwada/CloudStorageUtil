package com.evolveasia.aws;


import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

public class TransferUpdate {
    public static final int STATE_COMPLETED = 1;
    public static final int PROGRESSED_CHANGED = 2;

    public final int state;
    public final int id;
    public final TransferState transferState;
    public final long byteCount;
    public final long byteTotal;
    public final String url;

    public TransferUpdate(int state, int id, TransferState transferState, long byteCurrent, long byteTotal, String url) {
        this.state = state;
        this.id = id;
        this.transferState = transferState;
        this.byteCount = byteCurrent;
        this.byteTotal = byteTotal;
        this.url = url;
    }
}
