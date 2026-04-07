package com.lubover.singularity.api;

/**
 * Interceptor 是在 allocate 过程中可以予以截断插入逻辑的拦截器
 */
public interface Interceptor {

    void handle(Context context);
}
