# gCloud Storage Utils

An Android library to make sync on Google Cloud Storage easier.
Currently it only supports image uploads.

## Installation

```groovy
implementation "com.evolve:gcloud-storage-utils:1.0.2"
```

## Usage

```kotlin
// build the `AuthInfo` for upload 
val list = mutableListOf<String>()
        list.add("Url 1")
        list.add("Url 2")
        list.add("Url 3")
        var disposable = Flowable.fromIterable(list)
                .flatMap { count ->
                    val gcsMetaData = GCSMetaInfo.Builder().apply {
                        bucketName = Constants.BUCKET_NAME // GCS google cloud buckey
                        serviceConfig = assets.open(Constants.SERVER_KEY)  // GCS auth config file.
                        this.gcsStoragePath = getGCSStoragePath()
                        imageMetaInfo = GCSMetaInfo.ImageMetaInfo().apply {
                            this.imagePath = "image path"
                            this.mediaType ="media type" //GCSMetaInfo.ImageMetaInfo.TYPE_JPEG
                            this.metadata = imageExtraParams.toString()
                            compressLevel = 80 ("Compression ratio") // default value is 100
                            compressFormat = Bitmap.CompressFormat.JPEG //default value is Bitmap.CompressFormat.PNG
                            waterMarkInfo= // default value is null , GCSMetaInfo.WaterMarkInfo.EMPTY()
                        }
                    }.build()
                    return@flatMap uploadImageGCS(gcsMetaData)
                }
                .doOnNext {
                    print("next on call $it")
                }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())

                .subscribe({
                    println("Progress: $it")

                }, {
                    println("Image error: ${it.localizedMessage}")
                    println("image error stacktrace: ${it.printStackTrace()}:")
                    progressBar.visibility = View.GONE
                }, {
                    println("completed")
                    progressBar.visibility = View.GONE
                })
    
// consume the request object as you wish to
```

Have a look at the sample code included in the project as well.