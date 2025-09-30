#include <graal_isolate.h>
#include <stdio.h>

#include "kotlin-lib.h"

int main(int argc, char *argv[]) {
    printf("starting up!\n");

    graal_isolate_t *isolate = NULL;
    graal_isolatethread_t *thread = NULL;

    if (graal_create_isolate(NULL, &isolate, &thread) != 0) {
        printf("initialization error\n");
        return 1;
    }

    printf("graal initialized!\n");

    noop(thread);

    printf("noop called!\n");

    graal_tear_down_isolate(thread);
}
