package art.galushko.openapi.testgen.generator.template

import io.qameta.allure.Allure

/**
 * Wraps a block into an Allure step (test-only helper).
 */
public fun <T> step(name: String, action: () -> T): T {
    Allure.step(name)
    return action()
}


