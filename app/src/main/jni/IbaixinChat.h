/*
 * IBaixinChat.h
 *
 *  Created on: 2015年8月25日
 *      Author: huanghui1
 */

#ifndef IBAIXINCHAT_H_
#define IBAIXINCHAT_H_


/**
 * char* to jstring
 */
jstring stoJstring(jclass clazz, jmethodID methodId, jstring encoding, JNIEnv* env, const char* pat);


#endif /* IBAIXINCHAT_H_ */
