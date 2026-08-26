package com.learnova.elearning.module.course.repository;

import com.learnova.elearning.module.course.entity.CourseBullet;
import com.learnova.elearning.module.course.entity.enums.BulletType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseBulletRepository extends JpaRepository<CourseBullet, Long> {

    List<CourseBullet> findByCourse_IdOrderByBulletTypeAscPositionAsc(Long courseId);

    List<CourseBullet> findByCourse_IdAndBulletTypeOrderByPositionAsc(Long courseId, BulletType bulletType);

    long countByCourse_IdAndBulletType(Long courseId, BulletType bulletType);

    /** Xóa toàn bộ bullet của một nhóm trước khi ghi lại (replace theo nhóm). */
    @Modifying
    @Query("DELETE FROM CourseBullet b WHERE b.course.id = :courseId AND b.bulletType = :bulletType")
    void deleteByCourseIdAndBulletType(@Param("courseId") Long courseId, @Param("bulletType") BulletType bulletType);
}
