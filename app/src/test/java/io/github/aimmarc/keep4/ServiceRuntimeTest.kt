package io.github.aimmarc.keep4

import io.github.aimmarc.keep4.service.ServiceMode
import io.github.aimmarc.keep4.service.ServiceRuntime
import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceRuntimeTest {
    @Test
    fun updatePublishesModeAndDetailTogether() {
        ServiceRuntime.update(ServiceMode.ERROR, "permission denied")

        assertEquals(ServiceMode.ERROR, ServiceRuntime.snapshot.value.mode)
        assertEquals("permission denied", ServiceRuntime.snapshot.value.detail)
    }
}
