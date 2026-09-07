package com.learnova.elearning.integration.storage;

/**
 * Đo thời lượng thật (giây) của một video đã lưu trên storage, bằng cách đọc
 * trực tiếp box MP4 thay vì tin giá trị client khai (§7.5 design_us15_us17.md).
 */
public interface VideoDurationProbe {

    /**
     * @param storageKey key của object video/mp4 trên storage
     * @return thời lượng làm tròn theo giây
     * @throws com.learnova.elearning.common.exception.AppException nếu object không tồn tại
     *         hoặc không đọc được box moov/mvhd hợp lệ
     */
    int probeSeconds(String storageKey);
}
