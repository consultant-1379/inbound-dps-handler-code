/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.operators;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ejb.EJBException;
import javax.interceptor.InvocationContext;
import javax.persistence.OptimisticLockException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;


@RunWith(PowerMockRunner.class)
@PrepareForTest({ DpsRetryInterceptor.class })
public class DpsRetryInterceptorTest {

    private static final int MAX_RETRIES = 3;

    private static final Logger logger = LoggerFactory.getLogger(DpsRetryInterceptorTest.class);

    @Mock
    private InvocationContext mockContext;
    @Mock
    private Object mockObject;

    @InjectMocks
    private DpsRetryInterceptor dpsRetryInterceptor;

    @Test
    public void testRetryForOptimisticLockDuringPersistAttributesFailure() throws Exception {
        final Exception exception = new EJBException(new OptimisticLockException());
        when(mockContext.proceed()).thenThrow(exception);
        when(mockContext.getMethod()).thenReturn(dpsRetryInterceptor.getClass().getMethods()[0]);
        try {
            dpsRetryInterceptor.methodInterceptor(mockContext);
        } catch (final Exception e) {
            logger.info("Expected EJBException thrown after retries have completed.");
            assertTrue(e instanceof InboundDpsHandlerException);
            verify(mockContext, times(MAX_RETRIES)).proceed();
        }
    }

    @Test
    public void testRetryForOptimisticLockDuringPersistAttributesSuccessful() throws Exception {
        when(mockContext.getMethod()).thenReturn(dpsRetryInterceptor.getClass().getMethods()[0]);
        when(mockContext.proceed()).thenAnswer(new Answer<Object>() {
            private int count;

            @Override
            public Object answer(final InvocationOnMock invocation) {
                if (count == 0) {
                    count++;
                    throw new EJBException(new OptimisticLockException());
                }
                return mockObject;
            }
        });
        dpsRetryInterceptor.methodInterceptor(mockContext);
        verify(mockContext, times(2)).proceed();
    }
}
