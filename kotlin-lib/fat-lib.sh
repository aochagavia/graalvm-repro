set -euo pipefail
OUT_DIR=build-fatlib
rm -rf "$OUT_DIR" && mkdir -p "$OUT_DIR"/objs
cp native-image-tmp/SVM-1759319522581/kotlin-lib.o "$OUT_DIR"/objs/

GRAAL=/home/aochagavia/graalvm-native-image-repro/kotlin-lib/gradle/jdk/graalvm-community-openjdk-25+37.1
CSTATIC=$GRAAL/lib/static/linux-amd64/glibc
CLIBC=$GRAAL/lib/svm/clibraries/linux-amd64/glibc


for A in \
  "$CLIBC/liblibchelper.a" \
  "$CLIBC/libsvm_container.a" \
  "$CSTATIC/libnio.a" \
  "$CSTATIC/libnet.a" \
  "$CSTATIC/libjava.a" \
  "$CSTATIC/libzip.a" \
  "$CLIBC/libjvm.a"
do
  (cd "$OUT_DIR"/objs && ar x "$A")
done

# Also statically link zlib
ZLIB_PATH=$(
  printf 'int main(void){return 0;}\n' | \
    gcc -x c - -Wl,--verbose -Wl,-Bstatic -lz -Wl,-Bdynamic -o /dev/null 2>&1 \
    | sed -n 's/.*attempt to open \(.*libz\.a\) succeeded.*/\1/p' | head -n1
)

(cd "$OUT_DIR"/objs && ar x "$ZLIB_PATH")

ar rcs "$OUT_DIR/libkotlin.a" "$OUT_DIR"/objs/*.o
ranlib "$OUT_DIR/libkotlin.a"

# echo "Now link with: gcc your_app.o -L$OUT_DIR -lkotlin -lz -ldl -lpthread -lrt -o your_app"
