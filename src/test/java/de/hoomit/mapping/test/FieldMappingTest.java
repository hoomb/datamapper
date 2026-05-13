package de.hoomit.mapping.test;

import de.hoomit.mapping.mapper.DefaultDataMapper;
import de.hoomit.mapping.mapper.FieldMappingRegistry;
import de.hoomit.mapping.test.domain.AuditEvent;
import de.hoomit.mapping.test.domain.LegacyUserData;
import de.hoomit.mapping.test.domain.RegisterData;
import de.hoomit.mapping.test.domain.UserSignUpWsDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldMappingTest {

    private final DefaultDataMapper dataMapper = new DefaultDataMapper();

    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("FieldMappingRegistry")
    class RegistryTests {

        private final FieldMappingRegistry registry = new FieldMappingRegistry();

        @Test
        @DisplayName("returns empty map for class pair with no @FieldMapping")
        void noMappings() {
            final Map<String, String> renames =
                    registry.renamesFor(RegisterData.class, LegacyUserData.class);
            assertTrue(renames.isEmpty());
        }

        @Test
        @DisplayName("source-side annotation produces sourceField → pairedField")
        void sourceSideAnnotation() {
            final Map<String, String> renames =
                    registry.renamesFor(UserSignUpWsDTO.class, RegisterData.class);
            assertEquals("login", renames.get("uid"));
        }

        @Test
        @DisplayName("multiple @FieldMapping on one field — each targets correct paired class")
        void multipleMappingsResolveByPairedClass() {
            final Map<String, String> toRegister =
                    registry.renamesFor(UserSignUpWsDTO.class, RegisterData.class);
            final Map<String, String> toLegacy =
                    registry.renamesFor(UserSignUpWsDTO.class, LegacyUserData.class);

            assertEquals("login",    toRegister.get("uid"));
            assertEquals("username", toLegacy.get("uid"));
        }

        @Test
        @DisplayName("destination-side annotation produces pairedField → destField (reverse direction)")
        void destSideAnnotation() {
            final Map<String, String> renames =
                    registry.renamesFor(UserSignUpWsDTO.class, AuditEvent.class);
            assertEquals("actorId", renames.get("uid"));
        }

        @Test
        @DisplayName("results are cached across calls")
        void resultsAreCached() {
            final Map<String, String> first  =
                    registry.renamesFor(UserSignUpWsDTO.class, RegisterData.class);
            final Map<String, String> second =
                    registry.renamesFor(UserSignUpWsDTO.class, RegisterData.class);
            assertSame(first, second, "cached result should be the same instance");
        }

        @Test
        @DisplayName("unrelated class pair is not affected by other annotations on the source class")
        void unrelatedPairUnaffected() {
            final Map<String, String> renames =
                    registry.renamesFor(UserSignUpWsDTO.class, AuditEvent.class);
            assertEquals(1, renames.size());
            assertEquals("actorId", renames.get("uid"));
        }
    }

    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("End-to-end mapping with renames")
    class EndToEndTests {

        @Test
        @DisplayName("UserSignUpWsDTO → RegisterData copies uid to login")
        void dtoToDomain() {
            final UserSignUpWsDTO source = new UserSignUpWsDTO();
            source.setUid("jane.doe");
            source.setPassword("secret");
            source.setFirstName("Jane");
            source.setLastName("Doe");

            final RegisterData dest = dataMapper.map(source, RegisterData.class, "FULL");

            assertEquals("jane.doe", dest.getLogin());
            assertEquals("secret",   dest.getPassword());
            assertEquals("Jane",     dest.getFirstName());
            assertEquals("Doe",      dest.getLastName());
        }

        @Test
        @DisplayName("RegisterData → UserSignUpWsDTO copies login back to uid")
        void domainToDto() {
            final RegisterData source = new RegisterData();
            source.setLogin("john.smith");
            source.setPassword("hunter2");
            source.setFirstName("John");
            source.setLastName("Smith");

            final UserSignUpWsDTO dest = dataMapper.map(source, UserSignUpWsDTO.class, "FULL");

            assertEquals("john.smith", dest.getUid());
            assertEquals("hunter2",    dest.getPassword());
            assertEquals("John",       dest.getFirstName());
            assertEquals("Smith",      dest.getLastName());
        }

        @Test
        @DisplayName("UserSignUpWsDTO → LegacyUserData uses the second @FieldMapping (uid → username)")
        void dtoToLegacy() {
            final UserSignUpWsDTO source = new UserSignUpWsDTO();
            source.setUid("legacy.user");
            source.setPassword("oldpw");

            final LegacyUserData dest = dataMapper.map(source, LegacyUserData.class, "FULL");

            assertEquals("legacy.user", dest.getUsername());
            assertEquals("oldpw",       dest.getPassword());
        }

        @Test
        @DisplayName("Round-trip preserves the original value")
        void roundTrip() {
            final UserSignUpWsDTO original = new UserSignUpWsDTO();
            original.setUid("round.trip");
            original.setPassword("pw");
            original.setFirstName("R");
            original.setLastName("T");

            final RegisterData intermediate =
                    dataMapper.map(original, RegisterData.class, "FULL");
            final UserSignUpWsDTO restored =
                    dataMapper.map(intermediate, UserSignUpWsDTO.class, "FULL");

            assertEquals(original.getUid(),       restored.getUid());
            assertEquals(original.getPassword(),  restored.getPassword());
            assertEquals(original.getFirstName(), restored.getFirstName());
            assertEquals(original.getLastName(),  restored.getLastName());
        }

        @Test
        @DisplayName("Dest-side-only annotation works (AuditEvent declares mapping, DTO does not)")
        void destSideOnlyAnnotation() {
            final UserSignUpWsDTO source = new UserSignUpWsDTO();
            source.setUid("actor-42");

            final AuditEvent event = dataMapper.map(source, AuditEvent.class, "FULL");
            assertEquals("actor-42", event.getActorId());
        }

        @Test
        @DisplayName("Field-set filtering still applies after rename (descriptor uses destination name)")
        void renamePlusFieldSet() {
            final UserSignUpWsDTO source = new UserSignUpWsDTO();
            source.setUid("u");
            source.setPassword("p");

            final RegisterData dest = dataMapper.map(source, RegisterData.class, "login");

            assertEquals("u", dest.getLogin());
            assertNull(dest.getPassword());
        }
    }
}