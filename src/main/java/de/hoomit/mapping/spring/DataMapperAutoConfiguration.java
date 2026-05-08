package de.hoomit.mapping.spring;

import de.hoomit.mapping.annotation.WsDTOMapping;
import de.hoomit.mapping.mapper.DataMapper;
import de.hoomit.mapping.mapper.DefaultDataMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(DataMapper.class)
public class DataMapperAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataMapper dataMapper(final ApplicationContext ctx) {
        final DefaultDataMapper mapper = new DefaultDataMapper();
        mapper.registerBeans(ctx.getBeansWithAnnotation(WsDTOMapping.class).values());
        return mapper;
    }
}