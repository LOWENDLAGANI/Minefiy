package com.minetallest.minefiy.b2

import com.amazonaws.HttpMethod
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.minetallest.minefiy.BuildConfig
import com.minetallest.minefiy.data.StorageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Date

class B2Manager {

    private val s3Client: AmazonS3Client
    private val bucketName: String

    init {
        val accessKey = BuildConfig.B2_ACCESS_KEY_ID
        val secretKey = BuildConfig.B2_SECRET_ACCESS_KEY
        val endpoint = BuildConfig.B2_ENDPOINT
        bucketName = BuildConfig.B2_BUCKET_NAME

        val regionName = endpoint.split('.').getOrNull(1)
        require(!regionName.isNullOrBlank()) { "Could not parse region from endpoint: '$endpoint'." }

        val credentials = BasicAWSCredentials(accessKey, secretKey)

        s3Client = AmazonS3Client(credentials).apply {
            setEndpoint(endpoint)
            setSignerRegionOverride(regionName)
        }
    }

    suspend fun listMp3s(): List<StorageFile> = withContext(Dispatchers.IO) {
        s3Client.listObjects(bucketName).objectSummaries
            .filter { it.key.endsWith(".mp3", ignoreCase = true) }
            .map { StorageFile(it.key, it.eTag.removeSurrounding("\"")) }
    }

    suspend fun getPresignedUrl(key: String): String = withContext(Dispatchers.IO) {
        val expiration = Date().apply { time += 3600 * 1000 } // 1 hour
        val request = GeneratePresignedUrlRequest(bucketName, key).withMethod(HttpMethod.GET).withExpiration(expiration)
        s3Client.generatePresignedUrl(request).toString()
    }

    suspend fun uploadArtwork(songKey: String, artworkData: ByteArray): String = withContext(Dispatchers.IO) {
        val artworkKey = "artwork/$songKey.jpg"
        val metadata = ObjectMetadata().apply {
            contentType = "image/jpeg"
            contentLength = artworkData.size.toLong()
        }
        val inputStream = ByteArrayInputStream(artworkData)
        s3Client.putObject(bucketName, artworkKey, inputStream, metadata)
        s3Client.getResourceUrl(bucketName, artworkKey)
    }

    suspend fun uploadArtistArtwork(artistName: String, artworkData: ByteArray): String = withContext(Dispatchers.IO) {
        val artworkKey = "artists/$artistName.jpg"
        val metadata = ObjectMetadata().apply {
            contentType = "image/jpeg"
            contentLength = artworkData.size.toLong()
        }
        val inputStream = ByteArrayInputStream(artworkData)
        s3Client.putObject(bucketName, artworkKey, inputStream, metadata)
        s3Client.getResourceUrl(bucketName, artworkKey)
    }

    fun downloadFile(storageFile: StorageFile, destination: File) {
        val s3Object = s3Client.getObject(bucketName, storageFile.key)
        s3Object.objectContent.use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}