📋 SendGrid 설정 가이드:
1️⃣ SendGrid API 키 발급 (무료)
1.
https://signup.sendgrid.com/ 에서 계정 생성
2.
로그인 → Settings → API Keys 메뉴
3.
Create API Key 클릭
4.
Name: wemeetnow-auth-service, Permissions: Full Access 선택
5.
API 키 복사 (한 번만 표시됨! 꼭 저장)
2️⃣ Sender Identity 등록 (필수)
SendGrid는 발신자 확인이 필요합니다:
1.
Settings → Sender Authentication → Single Sender Verification
2.
발신자 이메일 등록 (예: noreply@wemeetnow.com 또는 개인 이메일)
3.
확인 이메일 받고 인증
3️⃣ Render 환경 변수 설정
Render 대시보드에서:
SENDGRID_API_KEY=SG.xxxxxxxxxxxxxxxxxxxxxxxxxx
SENDGRID_FROM_EMAIL=noreply@wemeetnow.com  (또는 인증된 이메일)
SENDGRID_FROM_NAME=WeMeetTalk
4️⃣ 재배포
./gradlew clean build -Pprofile=dev
Render에서 자동 배포되거나 수동으로 Manual Deploy 클릭
✅ 변경 사항:
•
✨ SendGrid HTTP API 사용 (SMTP 포트 차단 문제 해결)
•
🔧 build.gradle에 SendGrid 의존성 추가
•
📧 EmailServiceImpl을 SendGrid API로 전환
•
⚙️ SendGridConfig 설정 클래스 추가
테스트 준비가 되면 알려주세요!
Claude Sonnet 4.5 • 26.4 credits
그럼 google 메일발송 안쓰고 sendgrid 사용하는건가요? 예상되는 메일 발송 시 비용 발생은 얼마인가요?
Thinking
네, 맞습니다! Gmail SMTP 대신 SendGrid HTTP API를 사용하는 것입니다.
📊 SendGrid 비용 정보:
✅ 무료 티어 (Free Plan)
•
매일 100개 이메일 무료 발송
•
월 3,000개 (100개 × 30일)
•
신용카드 등록 불필요
•
영구 무료
💰 유료 플랜 (필요시)
•
Essentials ($19.95/월): 월 50,000개
•
Pro ($89.95/월): 월 100,000개
•
Premier ($349.95/월): 월 1,200,000개
📈 예상 비용 산정:
시나리오 1: 소규모 서비스
•
일 50명 회원가입 = 50개 이메일/일
•
무료 티어로 충분 ✅
시나리오 2: 중규모 서비스  
•
일 200명 회원가입 = 200개 이메일/일
•
월 6,000개 → Essentials ($19.95/월) 필요
시나리오 3: 대규모 서비스
•
일 1,000명 회원가입 = 1,000개 이메일/일
•
월 30,000개 → Essentials ($19.95/월) 로 가능
🆚 Gmail SMTP vs SendGrid 비교:
항목
Gmail SMTP
SendGrid
Render 무료 환경
❌ 포트 차단으로 불가능
✅ HTTP API로 작동
무료 발송량
일 500개 (이론상)
일 100개
안정성
낮음 (스팸 차단 위험)
높음 (전문 서비스)
발송 속도
느림
빠름
모니터링/통계
없음
제공 (열람률, 클릭률)
비용
무료
무료 → $19.95/월
💡 추천:
개발/테스트 단계: SendGrid 무료 티어로 충분 ✅
프로덕션 (일 100명 이하): 무료 티어 유지
프로덕션 (일 100명 이상): Essentials 플랜 ($19.95/월)
현재 상황: Render 무료 환경에서는 SendGrid가 유일한 실질적 해결책입니다.
더 진행할까요, 아니면 다른 대안을 찾아볼까요?