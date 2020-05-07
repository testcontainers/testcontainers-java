package org.testcontainers.spock

import groovy.transform.PackageScope
import org.spockframework.runtime.extension.IMethodInvocation
import org.testcontainers.lifecycle.TestDescription

/**
 * Spock specific implementation of a Testcontainers TestDescription.
 *
 * Filesystem friendly name is based on Specification and Feature.
 */
@PackageScope
class SpockTestDescription implements TestDescription {

    String specName
    String featureName

    static SpockTestDescription fromTestDescription(IMethodInvocation invocation) {
        return new SpockTestDescription([
            specName: invocation.spec.name,
            featureName: invocation.feature.name
        ])
    }

    @Override
    String getTestId() {
        return getFilesystemFriendlyName()
    }

    @Override
    String getFilesystemFriendlyName() {
        return [specName, featureName].collect {
            URLEncoder.encode(it, 'UTF-8')
        }.join('-')
    }
}
