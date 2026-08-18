package com.agrogestao.storage;

import com.agrogestao.config.MinioProperties;
import com.agrogestao.exception.ServiceUnavailableException;
import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
public class ObjectStorageService {

    private static final Logger log = LoggerFactory.getLogger(ObjectStorageService.class);

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private final Object bucketLock = new Object();
    private volatile boolean bucketReady;

    public ObjectStorageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public void put(String objectKey, byte[] data, String contentType) {
        ensureBucket();
        try {
            String type = contentType == null || contentType.isBlank()
                    ? "application/octet-stream"
                    : contentType;
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(data), data.length, -1)
                    .contentType(type)
                    .build());
        } catch (Exception ex) {
            log.warn("Falha ao gravar objeto no armazenamento");
            throw new ServiceUnavailableException("Armazenamento de arquivos indisponível");
        }
    }

    public byte[] get(String objectKey) {
        ensureBucket();
        try (GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(objectKey)
                .build())) {
            return response.readAllBytes();
        } catch (Exception ex) {
            log.warn("Falha ao ler objeto no armazenamento");
            throw new ServiceUnavailableException("Armazenamento de arquivos indisponível");
        }
    }

    public void move(String sourceKey, String targetKey) {
        copy(sourceKey, targetKey);
        delete(sourceKey);
    }

    public void copy(String sourceKey, String targetKey) {
        ensureBucket();
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(targetKey)
                    .source(CopySource.builder()
                            .bucket(properties.getBucket())
                            .object(sourceKey)
                            .build())
                    .build());
        } catch (Exception ex) {
            log.warn("Falha ao copiar objeto no armazenamento");
            throw new ServiceUnavailableException("Armazenamento de arquivos indisponível");
        }
    }

    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        ensureBucket();
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            log.warn("Falha ao excluir objeto no armazenamento");
            throw new ServiceUnavailableException("Armazenamento de arquivos indisponível");
        }
    }

    private void ensureBucket() {
        if (bucketReady) {
            return;
        }
        synchronized (bucketLock) {
            if (bucketReady) {
                return;
            }
            try {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                        .bucket(properties.getBucket())
                        .build());
                if (!exists) {
                    minioClient.makeBucket(MakeBucketArgs.builder()
                            .bucket(properties.getBucket())
                            .build());
                }
                bucketReady = true;
            } catch (Exception ex) {
                log.warn("Falha ao garantir bucket no armazenamento");
                throw new ServiceUnavailableException("Armazenamento de arquivos indisponível");
            }
        }
    }
}
