import type { Category } from '../types/category';
import type { CourseDetail, Curriculum, EnrolledCourse } from '../types/course';

export const MOCK_CATEGORIES: Category[] = [
  { id: 1, name: 'Phát triển Web & Lập trình', slug: 'web-development', courseCount: 12 },
  { id: 2, name: 'Trí tuệ nhân tạo & Data', slug: 'ai-data-science', courseCount: 8 },
  { id: 3, name: 'Thiết kế UI/UX', slug: 'ui-ux-design', courseCount: 6 },
  { id: 4, name: 'Quản trị Kinh doanh', slug: 'business', courseCount: 5 },
  { id: 5, name: 'Điện toán Đám mây & DevOps', slug: 'cloud-devops', courseCount: 7 }
];

export const MOCK_COURSES: CourseDetail[] = [
  {
    id: 101,
    title: 'Lập trình Backend chuyên nghiệp với Spring Boot & Microservices',
    slug: 'lap-trinh-backend-chuyen-nghiep-spring-boot',
    subtitle: 'Xây dựng hệ thống REST API hoàn chỉnh, bảo mật JWT, kết nối PostgreSQL, Docker và kiến trúc Microservices thực chiến.',
    description: `Khóa học đưa bạn đi từ những khái niệm nền tảng của Spring Boot đến khi triển khai được một REST API hoàn chỉnh chuẩn doanh nghiệp.
Bạn sẽ nắm vững: cấu trúc dự án theo module, kết nối PostgreSQL bằng JPA và Flyway, bảo mật với JWT và Spring Security, xử lý lỗi tập trung, viết unit test và triển khai ứng dụng lên Docker/Cloud.
Khóa học được thiết kế thực tế, nhiều bài tập thực hành sát với dự án thực tế.`,
    thumbnailUrl: 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800&auto=format&fit=crop&q=60',
    level: 'INTERMEDIATE',
    price: 499000,
    status: 'PUBLISHED',
    ratingAvg: 4.9,
    totalStudents: 1250,
    categoryId: 1,
    categoryName: 'Phát triển Web & Lập trình',
    category: MOCK_CATEGORIES[0],
    lecturerName: 'Nguyễn Văn Nam',
    learningObjectives: [
      'Xây dựng REST API hoàn chỉnh theo chuẩn RESTful với Spring Boot 3',
      'Thiết kế cơ sở dữ liệu quan hệ PostgreSQL và quản lý migration với Flyway',
      'Bảo mật API mạnh mẽ với Spring Security, JWT và Role-Based Access Control',
      'Tích hợp Docker, Redis Cache và triển khai hệ thống lên máy chủ thực tế'
    ],
    requirements: [
      'Có kiến thức Java cơ bản (OOP, Collection, Exception)',
      'Máy tính cài đặt JDK 21 trở lên và IDE (IntelliJ IDEA / VS Code)'
    ],
    targetAudiences: [
      'Sinh viên ngành CNTT muốn đi làm lập trình viên Backend',
      'Frontend developer muốn chuyển đổi hoặc học thêm Backend để trở thành Fullstack'
    ]
  },
  {
    id: 102,
    title: 'Khóa học React 19 & TypeScript: Từ Cơ bản đến Nâng cao',
    slug: 'khoa-hoc-react-19-typescript-tu-co-ban-den-nang-cao',
    subtitle: 'Làm chủ React Hooks mới nhất, Context API, Zustand, Tailwind CSS và kiến trúc Feature-Based hiện đại.',
    description: `Học cách xây dựng Single Page Application hiện đại với React 19, TypeScript và Tailwind CSS.
Khóa học tập trung vào tư duy component tái sử dụng, quản lý state hiệu quả và tối ưu hóa hiệu năng render.`,
    thumbnailUrl: 'https://images.unsplash.com/photo-1633356122544-f134324a6cee?w=800&auto=format&fit=crop&q=60',
    level: 'BEGINNER',
    price: 399000,
    status: 'PUBLISHED',
    ratingAvg: 4.8,
    totalStudents: 980,
    categoryId: 1,
    categoryName: 'Phát triển Web & Lập trình',
    category: MOCK_CATEGORIES[0],
    lecturerName: 'Trần Thị Mai',
    learningObjectives: [
      'Thành thạo React 19 Component lifecycle và các hooks hiện đại',
      'Áp dụng TypeScript để viết code an toàn, giảm thiểu bug',
      'Xây dựng giao diện responsive đẹp mắt với Tailwind CSS',
      'Quản lý state toàn cục với Zustand và Context API'
    ],
    requirements: [
      'Biết HTML, CSS và JavaScript ES6 cơ bản'
    ],
    targetAudiences: [
      'Người mới bắt đầu học lập trình Frontend',
      'Lập trình viên muốn nâng cấp kiến thức lên React 19 và TypeScript'
    ]
  },
  {
    id: 103,
    title: 'Thiết kế Trải nghiệm Người dùng UI/UX với Figma',
    slug: 'thiet-ke-trai-nghiem-nguoi-dung-ui-ux-voi-figma',
    subtitle: 'Quy trình thiết kế sản phẩm số chuyên nghiệp: User Research, Wireframing, Prototyping và Design System.',
    description: `Khóa học toàn diện về UI/UX Design. Bạn sẽ học cách nghiên cứu hành vi người dùng, xây dựng Wireframe, tạo Prototype sống động và thiết kế hệ thống Design System hoàn chỉnh trên Figma.`,
    thumbnailUrl: 'https://images.unsplash.com/photo-1581291518655-9523c93269e4?w=800&auto=format&fit=crop&q=60',
    level: 'ALL',
    price: 0,
    status: 'PUBLISHED',
    ratingAvg: 4.7,
    totalStudents: 2340,
    categoryId: 3,
    categoryName: 'Thiết kế UI/UX',
    category: MOCK_CATEGORIES[2],
    lecturerName: 'Lê Hoàng Phúc',
    learningObjectives: [
      'Nắm vững tư duy thiết kế lấy người dùng làm trung tâm (User-Centered Design)',
      'Sử dụng thành thạo công cụ Figma từ cơ bản đến Auto Layout, Components, Variants',
      'Xây dựng Design System có thể mở rộng cho dự án lớn',
      'Tạo Interactive Prototype để trình bày và thử nghiệm với người dùng'
    ],
    requirements: [
      'Không yêu cầu kinh nghiệm trước, chỉ cần máy tính có kết nối Internet'
    ],
    targetAudiences: [
      'Những ai muốn bắt đầu sự nghiệp UI/UX Designer',
      'Developer hoặc Product Manager muốn nâng cao gu thẩm mỹ và tư duy sản phẩm'
    ]
  },
  {
    id: 104,
    title: 'Thực chiến Điện toán Đám mây AWS & DevOps CI/CD',
    slug: 'thuc-chien-dien-toan-dam-may-aws-devops',
    subtitle: 'Triển khai hạ tầng đám mây với EC2, S3, RDS, Docker, Kubernetes và tự động hóa với GitHub Actions.',
    description: `Khóa học đưa bạn vào vai trò DevOps Engineer thực tế: cấu hình VPC, bảo mật hạ tầng, containerize ứng dụng với Docker, thiết lập cụm Kubernetes và xây dựng pipeline CI/CD tự động.`,
    thumbnailUrl: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&auto=format&fit=crop&q=60',
    level: 'ADVANCED',
    price: 699000,
    status: 'PUBLISHED',
    ratingAvg: 4.95,
    totalStudents: 620,
    categoryId: 5,
    categoryName: 'Điện toán Đám mây & DevOps',
    category: MOCK_CATEGORIES[4],
    lecturerName: 'Hoàng Minh Quân',
    learningObjectives: [
      'Làm chủ các dịch vụ cốt lõi của AWS (EC2, S3, RDS, IAM, CloudWatch)',
      'Đóng gói và triển khai ứng dụng bằng Docker và Kubernetes',
      'Xây dựng pipeline CI/CD hoàn chỉnh với GitHub Actions',
      'Tối ưu chi phí và nâng cao tính sẵn sàng cao (High Availability)'
    ],
    requirements: [
      'Có kiến thức về hệ điều hành Linux và mạng cơ bản'
    ],
    targetAudiences: [
      'Lập trình viên muốn nâng cao kỹ năng Cloud & DevOps',
      'Kỹ sư hệ thống muốn chuyển đổi sang môi trường Cloud'
    ]
  }
];

export const MOCK_CURRICULUM: Record<number, Curriculum> = {
  101: {
    courseId: 101,
    sections: [
      {
        id: 1,
        title: 'Chương 1: Giới thiệu & Cài đặt môi trường',
        description: 'Tổng quan kiến trúc Spring Boot và thiết lập môi trường phát triển',
        position: 1,
        lessons: [
          {
            id: 1011,
            title: '1.1 Tổng quan về Spring Boot 3 và Hệ sinh thái Spring',
            contentType: 'VIDEO',
            uploadStatus: 'READY',
            durationSeconds: 420,
            isPreview: true,
            position: 1,
            contentUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'
          },
          {
            id: 1012,
            title: '1.2 Hướng dẫn cài đặt JDK 21, IntelliJ IDEA và PostgreSQL',
            contentType: 'ARTICLE',
            uploadStatus: 'READY',
            isPreview: true,
            position: 2,
            contentText: `### Hướng dẫn cài đặt môi trường
1. **JDK 21**: Tải OpenJDK hoặc Eclipse Temurin từ trang chủ adoptium.net.
2. **PostgreSQL**: Cài đặt PostgreSQL 16 và pgAdmin 4.
3. **IDE**: Khuyên dùng IntelliJ IDEA Community hoặc Ultimate.
4. **Kiểm tra**: Mở terminal và chạy \`java -version\` và \`psql --version\`.`
          },
          {
            id: 1013,
            title: '1.3 Tài liệu đính kèm: CheatSheet Spring Boot Annotations',
            contentType: 'FILE',
            uploadStatus: 'READY',
            position: 3,
            originalFileName: 'SpringBoot_Annotations_CheatSheet.pdf',
            fileSizeBytes: 2048500,
            contentUrl: '#'
          }
        ]
      },
      {
        id: 2,
        title: 'Chương 2: Thiết kế REST API & Kết nối Database với JPA',
        description: 'Xây dựng các API CRUD, quan hệ bảng Entity và phân trang dữ liệu',
        position: 2,
        lessons: [
          {
            id: 1014,
            title: '2.1 Thiết kế Database và viết migration với Flyway',
            contentType: 'VIDEO',
            uploadStatus: 'READY',
            durationSeconds: 680,
            isPreview: false,
            position: 1,
            contentUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'
          },
          {
            id: 1015,
            title: '2.2 Xây dựng tầng Service, Repository và Controller',
            contentType: 'VIDEO',
            uploadStatus: 'READY',
            durationSeconds: 850,
            isPreview: false,
            position: 2,
            contentUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4'
          }
        ]
      },
      {
        id: 3,
        title: 'Chương 3: Bảo mật API với JWT & Spring Security',
        description: 'Cấu hình Filter Chain, AuthenticationProvider và phân quyền Endpoints',
        position: 3,
        lessons: [
          {
            id: 1016,
            title: '3.1 Luồng xác thực Access Token & Refresh Token',
            contentType: 'VIDEO',
            uploadStatus: 'READY',
            durationSeconds: 920,
            isPreview: false,
            position: 1,
            contentUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4'
          }
        ]
      }
    ]
  }
};

export const MOCK_ENROLLED_COURSES: EnrolledCourse[] = [
  {
    enrollmentId: 1,
    courseId: 101,
    courseTitle: 'Lập trình Backend chuyên nghiệp với Spring Boot & Microservices',
    courseSlug: 'lap-trinh-backend-chuyen-nghiep-spring-boot',
    courseThumbnailUrl: 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800&auto=format&fit=crop&q=60',
    lecturerName: 'Nguyễn Văn Nam',
    categoryName: 'Phát triển Web & Lập trình',
    level: 'INTERMEDIATE',
    progress: 40,
    status: 'ACTIVE',
    enrolledAt: '2026-08-20T10:00:00Z'
  }
];
