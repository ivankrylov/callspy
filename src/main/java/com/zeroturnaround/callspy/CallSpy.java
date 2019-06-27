package com.zeroturnaround.callspy;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.*;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class CallSpy implements ClassFileTransformer {

  static void attachAttribute(CtClass cc, CtMethod method) {
    ClassFile ccFile = cc.getClassFile();
    ConstPool constpool = ccFile.getConstPool();
    
    // create the annotation
    AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
    Annotation annot = new Annotation("Inline", constpool);
    //annot.addMemberValue("value", new IntegerMemberValue(ccFile.getConstPool(), 0));
    attr.addAnnotation(annot);
    method.getMethodInfo().addAttribute(attr); 
  }


  @Override
  public byte[] transform(//region other parameters
                          ClassLoader loader,
                          String className,
                          Class<?> classBeingRedefined,
                          ProtectionDomain protectionDomain,
                          //endregion
                          byte[] classfileBuffer) throws IllegalClassFormatException {

    ClassPool cp = ClassPool.getDefault();

    //region filter out non-application classes
    // Application filter. Can be externalized into a property file.
    // For instance, profilers use blacklist/whitelist to configure this kind of filters
    if (!className.startsWith("java/security/Provider")) {
      return classfileBuffer;
    }
    System.out.println("Found class java.security.Provider");
    //endregion

    try {
      CtClass ct = cp.makeClass(new ByteArrayInputStream(classfileBuffer));

      // Add annotation
      // https://stackoverflow.com/questions/2964180/adding-an-annotation-to-a-runtime-generated-method-class-using-javassist

      CtMethod[] declaredMethods = ct.getDeclaredMethods();
      for (CtMethod method : declaredMethods) {
        //region instrument method
          if (method.getName().equals("getEngineName")) {
              System.out.println("Attaching Force Inline attribute");
              attachAttribute(ct, method);
          }
        //endregion
      }

      return ct.toBytecode();
    } catch (Throwable e) {
      e.printStackTrace();
    }

    return classfileBuffer;
  }
}
