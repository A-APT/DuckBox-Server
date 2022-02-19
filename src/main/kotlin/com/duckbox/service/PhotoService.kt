package com.duckbox.service

import com.duckbox.domain.photo.Photo
import com.duckbox.domain.photo.PhotoRepository
import org.bson.types.Binary
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class PhotoService(private val photoRepository: PhotoRepository) {

    fun savePhoto(file: MultipartFile): ObjectId {
        return photoRepository.save(
            Photo(
                image = Binary(file.bytes)
            )).id
    }

    fun getPhoto(objectId: ObjectId): Binary {
        return photoRepository.findById(objectId).get().image
    }

    fun deletePhoto(objectId: ObjectId) {
        photoRepository.deleteById(objectId)
    }

}
