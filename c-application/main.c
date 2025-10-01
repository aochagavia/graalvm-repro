#include <graal_isolate.h>
#include <jni.h>
#include <stdio.h>

#include "kotlin-lib.h"

int run_in_isolate() {
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
    return 0;
}

int run_in_jni() {
    printf("starting up!\n");

    JNIEnv *env = NULL;
    JavaVM *jvm = NULL;

    printf("creating JVM!\n");

    JavaVMInitArgs vm_args;
    vm_args.version = JNI_VERSION_1_8;
    vm_args.nOptions = 0;
    vm_args.options = NULL;
    vm_args.ignoreUnrecognized = JNI_TRUE;

    if (JNI_CreateJavaVM(&jvm, (void **)&env, &vm_args) != JNI_OK) {
        printf("\nFailed to create a new Java VM\n");
        return 1;
    }

    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }

    printf("JVM created!\n");

    jclass myClass = (*env)->FindClass(env, "MyJavaClass");
    if (myClass == NULL) {
        printf("\nFailed to find class MyJavaClass\n");
        if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionDescribe(env); (*env)->ExceptionClear(env); }
        return 1;
    }

    // TODO: maybe the return type is wrong here (V = void?)
    jmethodID ctor = (*env)->GetMethodID(env, myClass, "<init>", "()V");
    if (ctor == NULL) {
        printf("\nFailed to get MyClass constructor\n");
        if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionDescribe(env); (*env)->ExceptionClear(env); }
        return 1;
    }

    jobject myClassObj = (*env)->NewObject(env, myClass, ctor);
    if (myClassObj == NULL) {
        printf("\nFailed to create MyObject instance\n");
        if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionDescribe(env); (*env)->ExceptionClear(env); }
        return 1;
    }

    jmethodID noop = (*env)->GetMethodID(env, myClass, "noop", "()V");
    if (noop == NULL) {
        printf("\nFailed to get noop method\n");
        if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionDescribe(env); (*env)->ExceptionClear(env); }
        (*env)->DeleteLocalRef(env, myClassObj);
        return 1;
    }

    (*env)->CallVoidMethod(env, myClassObj, noop);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        (*env)->DeleteLocalRef(env, myClassObj);
        return 1;
    }

    printf("noop called!\n");

    (*env)->DeleteLocalRef(env, myClassObj);
    (*env)->DeleteLocalRef(env, myClass);
    (*jvm)->DestroyJavaVM(jvm);

    return 0;
}

int main(int argc, char *argv[]) {
    printf("=== ISOLATE\n");
    int err = run_in_isolate();
    if (err) {
        return err;
    }

    printf("=== JNI");
    return run_in_jni();
}
