// 실행: mongosh "<connection-string>" scripts/seed/swipes-seed.js
// games 컬렉션에서 랜덤 5개를 뽑아 swipes 컬렉션에 라운드 1건을 생성한다.
// 라운드를 여러 건 만들고 싶으면 이 스크립트를 원하는 횟수만큼 반복 실행하면 된다.

const gameIdList = db.games
  .aggregate([{ $sample: { size: 5 } }, { $project: { _id: 1 } }])
  .toArray()
  .map((doc) => doc._id);

db.swipes.insertOne({
  gameIdList: gameIdList,
  createdAt: new Date(),
});
