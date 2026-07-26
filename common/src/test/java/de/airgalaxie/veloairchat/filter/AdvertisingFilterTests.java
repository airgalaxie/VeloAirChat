/*
 * This file is part of VeloAirChat, licensed under the Apache License 2.0.
 *
 *  Copyright (c) AirGalaxie/VeloAirChat contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package de.airgalaxie.veloairchat.filter;

import de.airgalaxie.veloairchat.user.TestOnlineUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AdvertisingFilterTests {

    AdvertisingFilterer filterer = new AdvertisingFilterer(new ChatFilter.FilterSettings());

    @Test
    public void testSentence() {
        Assertions.assertTrue(filterer.isAllowed(new TestOnlineUser(), "This is an example sentence!"));
    }

    @Test
    public void testFullUrl() {
        Assertions.assertFalse(filterer.isAllowed(new TestOnlineUser(), "https://example.com"));
    }

    @Test
    public void testPartialUrl() {
        Assertions.assertFalse(filterer.isAllowed(new TestOnlineUser(), "example.com"));
    }

    @Test
    public void testSubDomainUrl() {
        Assertions.assertFalse(filterer.isAllowed(new TestOnlineUser(), "chat.example.com"));
    }

    @Test
    public void testSubDomainUrlWithPort() {
        Assertions.assertFalse(filterer.isAllowed(new TestOnlineUser(), "example.com:25565"));
    }

    @Test
    public void testTopLevelDomain() {
        Assertions.assertTrue(filterer.isAllowed(new TestOnlineUser(), ".net"));
    }

}
