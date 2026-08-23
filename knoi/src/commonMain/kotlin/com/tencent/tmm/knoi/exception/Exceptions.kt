package com.tencent.tmm.knoi.exception

class FunctionNotRegisterException(methodName: String) : Exception("methodName = $methodName")
class ServiceNotRegisterException(service: String) : Exception("service = $service")
class ServiceRegisterException(message: String) : Exception(message)
class ServiceGetException(message: String) : Exception(message)
class DeclareNotRegisterException(name: String) : Exception("name = $name")

class ServiceFactoryFailException(service: String) : Exception("service = $service")
class ServiceProxyNotFoundException(service: String) :
    Exception("service proxy not found. service = $service")

class UnSupportTypeException(msg: String) : Exception(msg)

class ReturnTypeErrorException() : Exception("return type cannot be null, please use Unit::class.")