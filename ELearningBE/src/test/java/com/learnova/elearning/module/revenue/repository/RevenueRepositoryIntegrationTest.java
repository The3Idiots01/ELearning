package com.learnova.elearning.module.revenue.repository;

import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.payment.entity.PaymentOrder;
import com.learnova.elearning.module.payment.entity.enums.PaymentStatus;
import com.learnova.elearning.module.payment.repository.PaymentOrderRepository;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.entity.enums.UserRole;
import com.learnova.elearning.module.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RevenueRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private RevenueRepository revenueRepository;

    @Test
    void findCourseRevenue_includesZeroCoursesAndOnlyPaidOrdersForOwner() {
        User owner = userRepository.save(User.builder()
                .fullName("Revenue Owner")
                .email("revenue-owner-" + System.nanoTime() + "@test.local")
                .role(UserRole.USER)
                .build());
        User otherOwner = userRepository.save(User.builder()
                .fullName("Other Owner")
                .email("revenue-other-" + System.nanoTime() + "@test.local")
                .role(UserRole.USER)
                .build());

        Course sellingCourse = saveCourse(owner, "Selling course");
        Course emptyCourse = saveCourse(owner, "Empty course");
        Course otherCourse = saveCourse(otherOwner, "Other course");

        paymentOrderRepository.save(order(sellingCourse, owner, "100000.00", PaymentStatus.PAID, 1001L));
        paymentOrderRepository.save(order(sellingCourse, owner, "50000.00", PaymentStatus.PAID, 1002L));
        paymentOrderRepository.save(order(sellingCourse, owner, "900000.00", PaymentStatus.PENDING, 1003L));
        paymentOrderRepository.save(order(otherCourse, owner, "999000.00", PaymentStatus.PAID, 1004L));

        List<CourseRevenueProjection> result = revenueRepository.findCourseRevenue(owner.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCourseId()).isEqualTo(sellingCourse.getId());
        assertThat(result.get(0).getRevenue()).isEqualByComparingTo("150000.00");
        assertThat(result.get(1).getCourseId()).isEqualTo(emptyCourse.getId());
        assertThat(result.get(1).getRevenue()).isEqualByComparingTo("0");
    }

    private Course saveCourse(User owner, String title) {
        return courseRepository.save(Course.builder()
                .lecturer(owner)
                .title(title)
                .slug(title.toLowerCase().replace(' ', '-') + '-' + System.nanoTime())
                .status(CourseStatus.PUBLISHED)
                .price(new BigDecimal("100000.00"))
                .build());
    }

    private PaymentOrder order(Course course, User student, String amount, PaymentStatus status, Long orderCode) {
        return PaymentOrder.builder()
                .orderCode(orderCode)
                .student(student)
                .course(course)
                .amount(new BigDecimal(amount))
                .status(status)
                .build();
    }
}
