package org.minturtle.careersupport.common.config

import org.minturtle.careersupport.user.resolvers.UserInfoArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer


@Configuration
class WebFluxConfig(
    private val userInfoArgumentResolver: UserInfoArgumentResolver
) : WebFluxConfigurer {

    override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
        configurer.addCustomResolver(userInfoArgumentResolver)
    }
}