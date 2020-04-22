/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.advice;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Tag;

import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.beanutils.WrapDynaBean;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An interceptor that will set the createdBy and or modifiedBy fields on object being modified in
 * methods having @Validate. The object must either extend NetworkEntity, or be a Contact, Endpoint,
 * MachineTag, Tag, Identifier, or Comment. </br> This is only needed server side running the web
 * service tests, since the tests call the interface service method, not the HTTP resource method
 * where the createdBy and modifiedBy fields are actually set.
 */
public class TestValidateInterceptor {

  private static final Logger LOG = LoggerFactory.getLogger(TestValidateInterceptor.class);

  @Around(
      value =
          "execution(* *(@org.springframework.validation.annotation.Validated (*))) && args(arg) ||"
              + "execution(* *(..,@org.springframework.validation.annotation.Validated (*))) && args(..,arg)"
              + "execution(* *(..,..,@org.springframework.validation.annotation.Validated (*))) && args(..,..,arg)"
              + "execution(* *(..,..,..,@org.springframework.validation.annotation.Validated (*))) && args(..,..,..,arg)"
              + "execution(* *(..,..,..,..,@org.springframework.validation.annotation.Validated (*))) && args(..,..,..,..,arg)"
              + "execution(* *(..,..,..,..,..,@org.springframework.validation.annotation.Validated (*))) && args(..,..,..,..,..,arg)")
  public Object inspect(ProceedingJoinPoint pjp, Object arg) throws Throwable {
    if (arg instanceof NetworkEntity
        || arg instanceof Comment
        || arg instanceof Contact
        || arg instanceof Endpoint
        || arg instanceof Identifier
        || arg instanceof MachineTag
        || arg instanceof Tag
        || arg instanceof Institution
        || arg instanceof Collection
        || arg instanceof Person) {
      addRequiredFields(arg);
    }
    return pjp.proceed();
  }

  /**
   * Add required fields createdBy and modifiedBy (where possible) to target object which either
   * extends NetworkEntity or is a Contact, Endpoint, MachineTag, Tag, Identifier, or Comment.
   *
   * @param target object
   */
  private void addRequiredFields(Object target) {
    WrapDynaBean wrapped = new WrapDynaBean(target);
    DynaClass dynaClass = wrapped.getDynaClass();
    // update createdBy field
    if (dynaClass.getDynaProperty("createdBy") != null) {
      wrapped.set("createdBy", "WS TEST");
    }
    // update modifiedBy field
    if (dynaClass.getDynaProperty("modifiedBy") != null) {
      wrapped.set("modifiedBy", "WS TEST");
    }
  }
}
