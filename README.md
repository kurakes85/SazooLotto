🔮 사주로또 (SazooLotto) - v1.0.0

"당신의 운세로 찾는 행운의 번호" > 복잡한 건 싫다! 4060 중장년층을 위한 가장 쉽고 편안한 로또 추천 앱

📱 프로젝트 소개

사주로또는 사용자의 사주 정보(생년월일, 태어난 시간)를 분석하여 오행(五行) 기반의 로또 번호를 추천해 주는 안드로이드 애플리케이션입니다.

기존 앱들의 복잡한 UI와 작은 글씨에 지친 40~60대 사용자를 위해, "큰 글씨, 직관적인 버튼, 로그인 없는 즉시 사용"을 핵심 UX 원칙으로 설계했습니다.

✨ 주요 기능

* **📡 실시간 당첨 확인**: 동행복권 공식 API를 연동하여 앱 실행 시 자동으로 최신 회차 당첨 번호를 보여줍니다. (Retrofit2)
* **🎨 직관적인 디자인**: 번호 구간별(1~10번대 등) 고유 색상을 적용하여 실제 로또 공과 동일한 시각적 경험 제공.
* **📅 간편한 사주 입력**: 연/월/일 및 태어난 시간대(새벽/오전/오후 등)를 큼직한 선택지로 쉽게 입력.
* **🍀 오행 기반 번호 추천**: 사주 알고리즘(SazooEngine)을 통해 나에게 맞는 로또 번호 6개 생성.
* **💌 결과 공유**: 추천된 번호와 운세를 가족이나 지인에게 카카오톡/문자로 공유.

📅 간편한 사주 입력: 연/월/일 및 태어난 시간대(새벽/오전/오후 등)를 큼직한 선택지로 쉽게 입력.

🍀 오행 기반 번호 추천: 사주 알고리즘(SazooEngine)을 통해 나에게 맞는 로또 번호 6개 생성.

🚫 액운 숫자 제외 (리워드 광고): 광고를 시청하면 나쁜 기운의 숫자를 제외하고 번호를 정제해 주는 게이미피케이션 요소.

💌 결과 공유: 추천된 번호와 운세를 가족이나 지인에게 카카오톡/문자로 공유.

👁️ 시니어 친화적 UI: 18sp 이상의 큰 폰트, 60dp 이상의 넉넉한 터치 영역, 명확한 색상 대비.

🛠 기술 스택 (Tech Stack)

* **Network**: Retrofit2, Gson (동행복권 API 통신)
* **UI**: Jetpack Compose (Material3)
* **Language**: Kotlin
* **Monetization**: Google AdMob (Rewarded Ads)

Language: Kotlin

UI Framework: Jetpack Compose (Material3)

Architecture: MVVM Pattern (State Hoisting), Single Activity

Monetization: Google AdMob (Rewarded Ads)

Etc: Java Time API (Desugaring), Coroutines

📂 프로젝트 구조

com.example.sazoolotto
├── MainActivity.kt          // 앱 진입점 및 광고 매니저 연결
├── AdManager.kt             // AdMob 리워드 광고 로드/표시 관리
├── logic
│   ├── FortuneModel.kt      // 사주 및 로또 데이터 모델
│   └── SazooEngine.kt       // 오행 분석 및 번호 생성 알고리즘
└── ui
    ├── SazooLottoScreen.kt  // 메인 화면 (State & Layout)
    └── components           // 재사용 가능한 UI 컴포넌트 모음


🚀 트러블슈팅 (Dev Log)

개발 과정에서 발생한 주요 이슈와 해결 과정입니다.

1. Gradle Kotlin 플러그인 버전 충돌

문제: Android Studio 최신 버전 생성 시 Kotlin 2.0.21이 적용되나, 기존 설정 코드(1.9.x 스타일)와 충돌하여 빌드 실패.

해결:

Project 수준과 App 수준의 build.gradle.kts 역할을 명확히 분리.

composeOptions { kotlinCompilerExtensionVersion ... } 블록 삭제 (Kotlin 2.0부터 불필요).

플러그인 버전을 2.0.21로 통일하여 해결.

2. 소스 코드 중복 선언 (Redeclaration)

문제: 파일 이동 과정에서 src/main/java/ 루트와 패키지 폴더 내부에 동일한 파일이 중복 존재하여 컴파일 에러 발생.

해결: 패키지 구조(com.example.sazoolotto)를 준수하도록 파일을 정리하고 루트 경로의 중복 파일 완전 삭제.

📅 추후 업데이트 계획 (Roadmap)

[x] v1 (MVP): 사주 로직, 기본 UI, 리워드 광고 연동 완료

[ ] v2: 데이터 저장 (DataStore) - 매번 생년월일 입력하는 번거로움 제거

[ ] v2: 광고 제거 인앱 결제 추가

[ ] v3: 실제 로또 당첨 번호 API 연동 및 당첨 확인 기능

👨‍💻 SpreadLabs

※ 본 서비스는 오락용 참고 서비스이며, 실제 복권의 당첨을 보장하지 않습니다.
