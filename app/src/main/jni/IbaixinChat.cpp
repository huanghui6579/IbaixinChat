#include <jni.h>
#include <string.h>
#include <dirent.h>
#include <android/log.h>

#include "IbaixinChat.h"
#include "net_ibaixin_chat_util_NativeUtil.h"

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "NativeUtil", __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, "NativeUtil", __VA_ARGS__))

/*
 * Class:     net_ibaixin_chat_util_NativeUtil
 * Method:    listFileNames
 * Signature: (Ljava/lang/String;)Ljava/util/ArrayList;
 */
JNIEXPORT jobject JNICALL Java_net_ibaixin_chat_util_NativeUtil_listFileNames
  (JNIEnv * env, jclass clazz, jstring rootPath) {
	jclass list_cls = env->FindClass("java/util/ArrayList");//获得ArrayList类引用

	if(list_cls == NULL)
	{
		LOGI("list_cls is null");
		return NULL;
	}
	jmethodID list_costruct = env->GetMethodID(list_cls , "<init>","()V"); //获得得构造函数Id
	jobject list_obj = env->NewObject(list_cls , list_costruct); //创建一个Arraylist集合对象
	//或得Arraylist类中的 add()方法ID，其方法原型为： boolean add(Object object) ;
	jmethodID list_add  = env->GetMethodID(list_cls,"add","(Ljava/lang/Object;)Z");


	DIR *dir;
	struct dirent *drnt;
	const char* str;
	str = env->GetStringUTFChars(rootPath, JNI_FALSE);
	if(str == NULL)
	{
	   return NULL; /* OutOfMemoryError already thrown */
	}
//	env->ReleaseStringUTFChars(rootPath, str);
	dir = opendir(str);
	jclass strClass = env->FindClass("java/lang/String");
	jmethodID ctorID = env->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
	const char* encode = "utf-8";
	jstring encoding = env->NewStringUTF(encode);
	while ((drnt = readdir(dir)) != NULL)
	{
		const char* name = drnt->d_name;
		unsigned char type = drnt->d_type;
		if ((strcmp(".", name) == 0 || strcmp("..", name) == 0) && type == DT_DIR)
		{
			continue;
		}
		jstring result = stoJstring(strClass, ctorID, encoding, env, name);
		env->CallBooleanMethod(list_obj, list_add, result);	//执行Arraylist类实例的add方法，添加一个string对象
		env->DeleteLocalRef(result);
	}
	closedir(dir);
	env->ReleaseStringUTFChars(rootPath, str);
	env->DeleteLocalRef(encoding);
	env->DeleteLocalRef(strClass);
	env->DeleteLocalRef(rootPath);
	return list_obj;

}

/**
 * char* to jstring
 */
jstring stoJstring(jclass clazz, jmethodID methodId, jstring encoding, JNIEnv* env, const char* pat)
{
	/*jclass strClass = env->FindClass("Ljava/lang/String;");
	jmethodID ctorID = env->GetMethodID(strClass, "<init>",
			"([BLjava/lang/String;)V");*/
	jsize length = strlen(pat);
	jbyteArray bytes = env->NewByteArray(length);
	env->SetByteArrayRegion(bytes, 0, length, (jbyte*) pat);
	jstring result = (jstring) env->NewObject(clazz, methodId, bytes, encoding);
	jbyte* ba = env->GetByteArrayElements(bytes, JNI_FALSE);
	env->ReleaseByteArrayElements(bytes, ba, 0);
	env->DeleteLocalRef(bytes);
	return result;
}
