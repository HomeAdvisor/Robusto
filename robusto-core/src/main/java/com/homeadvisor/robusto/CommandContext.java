/*
 * Copyright 2016 HomeAdvisor, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.homeadvisor.robusto;

/**
 * Contains data that is applicable for the duration of the execution of a
 * single. Provides access to the command name and other data associated
 * with the ApiCommand. Provides a bridge between the thread that creates
 * the ApiCommand and the thread that executes it.
 */
public interface CommandContext
{
   /**
    * Get the logical name.
    * @return Logical command name.
    */
   String getCommandName();

   /**
    * Looks up the value associated with the given key from the attributes.
    * @param key Key to lookup in command data map.
    * @return Value associated with the given key, or null if none was set.
    */
   Object getCommandAttribute(String key);

   /**
    * Sets the given key/value pair for the underlying command attributes. Will
    * overwrite existing key if it exists.
    * @param key Key name to use.
    * @param val Value to associate with the key.
    */
   void setCommandAttribute(String key, Object val);

   /**
    * Removes the given key, if it exists, from the underlying command attributes.
    * This is a no-op if the key doesn't exist.
    * @param key Key to remove.
    */
   void removeCommandAttribute(String key);
}
