package art.galushko.openapi.testgen.generation

import io.qameta.allure.Allure

/**
 * Wraps a block into an Allure step (test-only helper).
 */
public fun <T> step(name: String, action: () -> T): T {
    Allure.step(name)
    return action()
}


