tag=$(date +"%Y%m%d%H%M")

lein ring uberjar
docker build -t rinha-2024q1-crebito-clojure:$tag -t zanfranceschi/rinha-2024q1-crebito-clojure:$tag .
docker push zanfranceschi/rinha-2024q1-crebito-clojure:$tag

echo $tag