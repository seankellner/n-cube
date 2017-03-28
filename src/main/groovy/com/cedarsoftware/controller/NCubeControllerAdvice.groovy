package com.cedarsoftware.controller

import com.cedarsoftware.util.io.JsonWriter
import groovy.transform.CompileStatic
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.Method

/**
 * Before Advice that sets user ID on current thread.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License")
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
@CompileStatic
@Aspect
class NCubeControllerAdvice
{
    private final NCubeController controller
    private static final Logger LOG = LoggerFactory.getLogger(NCubeControllerAdvice.class)

    NCubeControllerAdvice(NCubeController controller)
    {
        this.controller = controller
    }

    @Around("execution(* com.cedarsoftware.controller.NCubeController.*(..)) && !execution(* com.cedarsoftware.controller.NCubeController.getUserForDatabase(..))")
    def advise(ProceedingJoinPoint pjp)
    {
        try
        {
            // Place user on ThreadLocal
            controller.userForDatabase

            long start = System.nanoTime()
            // Execute method
            def ret = pjp.proceed()
            long end = System.nanoTime()
            long time = Math.round((end - start) / 1000000.0d)

            if (time > 1000)
            {
                LOG.info("[SLOW ${time}ms] ${getLogMessage(pjp)}")
            }
            else if (LOG.debugEnabled)
            {
                LOG.debug(getLogMessage(pjp))
            }
            return ret
        }
        catch (Exception e)
        {
            // If there were any exceptions, signal controller (which signals command servlet)
            controller.fail(e)
            return null
        }
    }

    private static String getLogMessage(ProceedingJoinPoint pjp)
    {
        MethodSignature signature = (MethodSignature) pjp.signature
        Method method = signature.method
        Object[] args = pjp.args
        StringBuilder sb = new StringBuilder()
        for (Object arg : args)
        {
            sb.append(getJsonStringToMaxLength(arg))
            sb.append('  ')
        }
        String jsonArgs = sb.toString().trim()
        return "${method.name}(${jsonArgs})"
    }

    private static String getJsonStringToMaxLength(Object obj)
    {
        String arg = JsonWriter.objectToJson(obj, [(JsonWriter.TYPE):false, (JsonWriter.SHORT_META_KEYS):true] as Map)
        if (arg.length() > 51)
        {
            arg = "${arg.substring(0, 50)}..."
        }
        return arg
    }
}