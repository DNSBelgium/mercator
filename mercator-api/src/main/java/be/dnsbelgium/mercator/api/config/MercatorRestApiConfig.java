package be.dnsbelgium.mercator.api.config;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class MercatorRestApiConfig implements RepositoryRestConfigurer {

  @Autowired
  private List<EntityManager> entityManagers;

  private static final Logger logger = getLogger(MercatorRestApiConfig.class);

  @Override
  public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
    logger.info("config.exposeRepositoryMethodsByDefault={}", config.exposeRepositoryMethodsByDefault());

    List<Class<?>> entityClasses = getAllManagedEntityTypes(entityManagers);
    for (Class<?> entityClass : entityClasses) {
      logger.info("Exposing ID's for {}", entityClass);
      config.exposeIdsFor(entityClass);
    }
  }

  private List<Class<?>> getAllManagedEntityTypes(List<EntityManager> entityManagers) {
    List<Class<?>> entityClasses = new ArrayList<>();

    entityManagers.forEach(entityManager -> {
      Metamodel metamodel = entityManager.getMetamodel();
      for (ManagedType<?> managedType : metamodel.getManagedTypes()) {
        Class<?> javaType = managedType.getJavaType();
        if (javaType.isAnnotationPresent(Entity.class)) {
          entityClasses.add(managedType.getJavaType());
        }
      }
    });

    return entityClasses;
  }

}
