// 실행: mongosh "<connection-string>" scripts/seed/games-seed.js

db.games.insertMany([
  {
    imageUrl:
      "https://d2x8kymwjom7h7.cloudfront.net/live/application_no/119001/application_no/119001/images/ss01_1734485016030.jpg",
    genre: "슈팅",
    releaseDate: ISODate("2025-01-21"),
    title: "텐가이",
    aiComment: "손끝이 짜릿한 슈팅 액션",
    storeUrl: "https://store.onstove.com/en/games/4639",
    communityUrl:
      "https://store.onstove.com/en/games/4639/community/132568/list?page=1&direction=LATEST",
    createdAt: new Date(),
    updatedAt: new Date(),
  },
  {
    imageUrl:
      "https://d2x8kymwjom7h7.cloudfront.net/live/member_no/240739460/application_no/119001/images/World_5.jpg",
    genre: "RPG",
    releaseDate: ISODate("2025-11-20"),
    title: "드로바 - 포세이큰 킨",
    aiComment: "어둠 속을 헤매는 다크 판타지",
    storeUrl: "https://store.onstove.com/en/games/101821",
    communityUrl:
      "https://store.onstove.com/en/games/101821/community/139354/list?page=1&direction=LATEST",
    createdAt: new Date(),
    updatedAt: new Date(),
  },
  {
    imageUrl:
      "https://d2x8kymwjom7h7.cloudfront.net/live/application_no/119001/application_no/119001/images/%ED%95%9C%EC%A4%84%EC%86%8C%EA%B0%9C_1738902427395.png",
    genre: "리듬",
    releaseDate: ISODate("2025-07-11"),
    title: "식스타 게이트: 스타게이저",
    aiComment: "손가락이 춤추는 리듬 게임",
    storeUrl: "https://store.onstove.com/en/games/4877",
    communityUrl:
      "https://store.onstove.com/en/games/4877/community/133668/list?page=1&direction=LATEST",
    createdAt: new Date(),
    updatedAt: new Date(),
  },
  {
    imageUrl:
      "https://d2x8kymwjom7h7.cloudfront.net/live/application_no/119001/images/Copy%20of%20EternalStrands_ArkoftheForge_1733391994307.jpg",
    genre: "액션",
    releaseDate: ISODate("2025-01-29"),
    title: "이터널 스트랜드 (Eternal Strands)",
    aiComment: "손맛 가득한 액션 어드벤처",
    storeUrl: "https://store.onstove.com/en/games/4400",
    communityUrl:
      "https://store.onstove.com/en/games/4400/community/129940/list?page=1&direction=LATEST",
    createdAt: new Date(),
    updatedAt: new Date(),
  },
  {
    imageUrl:
      "https://d2x8kymwjom7h7.cloudfront.net/live/application_no/119001/application_no/119001/images/4_1742879402701.jpg",
    genre: "RPG",
    releaseDate: ISODate("2025-04-24"),
    title: "클레르 옵스퀴르: 33 원정대",
    aiComment: "턴제 전투의 예술적 감동",
    storeUrl: "https://store.onstove.com/en/games/5102",
    communityUrl:
      "https://store.onstove.com/en/games/5102/community/134659/list?page=1&direction=LATEST",
    createdAt: new Date(),
    updatedAt: new Date(),
  },
  {
    imageUrl:
      "https://d2x8kymwjom7h7.cloudfront.net/live/member_no/218734367/application_no/119001/images/%ED%95%9C%20%EC%A4%84%20%EC%86%8C%EA%B0%9C_ts_1763734256780.png",
    genre: "비주얼 노벨",
    releaseDate: ISODate("2025-11-28"),
    title: "나의 별난 스트리머!",
    aiComment: "몰입감 넘치는 스토리 노벨",
    storeUrl: "https://store.onstove.com/en/games/102586",
    communityUrl:
      "https://store.onstove.com/en/games/102586/community/141746/list?page=1&direction=LATEST",
    createdAt: new Date(),
    updatedAt: new Date(),
  },
  {
    imageUrl:
      "https://d2x8kymwjom7h7.cloudfront.net/live/application_no/SGS0104044/images/Screenshot_167_ts_1779948829443.png",
    genre: "RPG",
    releaseDate: ISODate("2026-06-16"),
    title: "도키몬: 퀘스트 (Dokimon: Quest)",
    aiComment: "귀여운 몬스터 수집 RPG",
    storeUrl: "https://store.onstove.com/en/games/104658",
    communityUrl:
      "https://store.onstove.com/en/games/104658/community/146328/list?page=1&direction=LATEST",
    createdAt: new Date(),
    updatedAt: new Date(),
  },
  {
    imageUrl:
      "https://d2x8kymwjom7h7.cloudfront.net/live/application_no/119001/images/indie-studio-v3/1_1752224684194.webp",
    genre: "RPG",
    releaseDate: ISODate("2025-08-27"),
    title: "아르티스 임팩트 (Artis Impact)",
    aiComment: "손에 땀 쥐는 액션 RPG",
    storeUrl: "https://store.onstove.com/en/games/101699",
    communityUrl:
      "https://store.onstove.com/en/games/101699/community/138988/list?page=1&direction=LATEST",
    createdAt: new Date(),
    updatedAt: new Date(),
  },
]);
