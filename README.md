ELB Auth printer

[jinLibs]
빅솔론 프린터 드라이버

[libs]
acssmc-1.1.4.jar    // nfc reader module
bixolon_printer...jar   
libcommon_... jar



echo "# com-elb-cordova-plugin-kscat" >> README.md
git init
git add README.md
git commit -m "first commit"
git branch -M main
git remote add origin https://github.com/EverybodyBusiness/com-elb-cordova-plugin-kscat.git
git push -u origin main


[수정하고 push 할때에]

git add .
git commit -m "1.2.0"
git push -u origin main


[npm 생성 안내]
https://chatgpt.com/share/161ef7c3-20fc-44d3-91bb-a9fe2f7777ae
핵심은, package.json 에 등록된 package 의 이름과 npm 계정의 이름이 같아야 한다

@bookingtong/eap, @bookingtong/kscat 패키지가, bookingtong 계정에 등록되었다

npm login, bookingtong 계정으로

만약 패키지가 공개 범위(Scope)로 배포되어야 한다면, 패키지 이름을 @username/package-name 형태로 설정하고, 공개 범위로 배포하려면 다음과 같이 명령어를 사용합니다.
npm publish --access public

npm version major
npm version minor
npm version patch

npm version patch는 패치 버전을 증가시킵니다. (예: 1.0.0 → 1.0.1)
npm version minor는 마이너 버전을 증가시킵니다. (예: 1.0.0 → 1.1.0)
npm version major는 메이저 버전을 증가시킵니다. (예: 1.0.0 → 2.0.0)

https://chatgpt.com/c/66dbb2f5-3f60-8010-aa15-dc213a6bcc2d
git push origin --tags
npm publish

npm publish --access public
