# --- !Ups
CREATE TABLE stocks (
  "id" VARCHAR NOT NULL PRIMARY KEY,
  "code" VARCHAR NOT NULL,
  "name" VARCHAR NOT NULL,
  "marcket" INT NOT NULL
);

CREATE TABLE daily_reports (
  "stock_id" VARCHAR NOT NULL,
  "date" DATE NOT NULL,
  "open" DECIMAL,
  "close" DECIMAL,
  "high" DECIMAL,
  "row" DECIMAL
);

# --- !Downs
DROP TABLE "stocks";
DROP TABLE "daily_reports";
