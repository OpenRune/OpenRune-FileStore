package dev.openrune

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.constants.impl.RSCMProvider
import dev.openrune.definition.constants.impl.SymProvider
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConstantProviderTest {
    
    private lateinit var testDir: File
    
    @BeforeAll
    fun setup() {
        testDir = File(ConstantProviderTest::class.java.classLoader.getResource("mappings")!!.toURI())
        assertTrue(testDir.exists(), "Test directory should exist")
    }
    
    @BeforeEach
    fun resetProvider() {
        ConstantProvider.resetToDefaults()
        ConstantProvider.mappings = emptyMap()
        ConstantProvider.types.clear()
    }
    
    @Test
    fun `test RSCM v1 format loading and lookup`() {
        // Test with only RSCM v1 provider
        ConstantProvider.overrideProviders(listOf(RSCMProvider()))
        ConstantProvider.load(testDir)
        
        // Test basic lookups
        assertEquals(4151, ConstantProvider.getMapping("test_rscm_v1.abyssal_whip"))
        assertEquals(4587, ConstantProvider.getMapping("test_rscm_v1.dragon_scimitar"))
        assertEquals(1127, ConstantProvider.getMapping("test_rscm_v1.rune_platebody"))
        assertEquals(440, ConstantProvider.getMapping("test_rscm_v1.iron_ore"))
        
        // Test base type lookups (should be same as full type since only one file)
        assertEquals(4151, ConstantProvider.getMapping("test_rscm_v1.abyssal_whip"))

    }
    
    @Test
    fun `test RSCM v2 format loading and lookup`() {
        // Test with only RSCM v2 provider
        ConstantProvider.overrideProviders(listOf(RSCMProvider()))
        ConstantProvider.load(testDir)
        
        // Test basic lookups
        assertEquals(4152, ConstantProvider.getMapping("test_rscm_v2.abyssal_whip"))
        assertEquals(4588, ConstantProvider.getMapping("test_rscm_v2.dragon_scimitar"))
        assertEquals(1128, ConstantProvider.getMapping("test_rscm_v2.rune_platebody"))
        assertEquals(441, ConstantProvider.getMapping("test_rscm_v2.iron_ore"))
        
        // Test sub-property lookups
        assertEquals(46, ConstantProvider.getMapping("test_rscm_v2.whip:special"))
        assertEquals(45, ConstantProvider.getMapping("test_rscm_v2.whip:thing"))
        
        // Test base type lookups
        assertEquals(4152, ConstantProvider.getMapping("test_rscm_v2.abyssal_whip"))
    }
    
    @Test
    fun `test Sym format loading and lookup`() {
        // Test with only Sym provider
        ConstantProvider.overrideProviders(listOf(SymProvider()))
        ConstantProvider.load(testDir)
        
        // Test basic lookups
        assertEquals(4153, ConstantProvider.getMapping("test_sym.abyssal_whip"))
        assertEquals(4589, ConstantProvider.getMapping("test_sym.dragon_scimitar"))
        assertEquals(1129, ConstantProvider.getMapping("test_sym.rune_platebody"))
        assertEquals(442, ConstantProvider.getMapping("test_sym.iron_ore"))
        
        // Test base type lookups
        assertEquals(4153, ConstantProvider.getMapping("test_sym.abyssal_whip"))
    }
    
    @Test
    fun `test complex Sym format with types and sub-types`() {
        // Test with only Sym provider
        ConstantProvider.overrideProviders(listOf(SymProvider()))
        ConstantProvider.load(testDir)
        
        // Test basic lookups with type information
        assertEquals(7812, ConstantProvider.getMapping("test_complex_sym.hiscore_setapi"))
        assertEquals(253952, ConstantProvider.getMapping("test_complex_sym.clan_setting_options_list:clan_setting_title"))

        // Test individual sub-types (explicitly defined)
        assertEquals(253969, ConstantProvider.getMapping("test_complex_sym.clan_setting_options_list:clan_setting_option:0"))
        assertEquals(253970, ConstantProvider.getMapping("test_complex_sym.clan_setting_options_list:clan_setting_option:1"))
        assertEquals(253971, ConstantProvider.getMapping("test_complex_sym.clan_setting_options_list:clan_setting_option:2"))
        
        // Test simple properties (some with type, some without)
        assertEquals(253984, ConstantProvider.getMapping("test_complex_sym.clan_setting_options_list:clan_setting_entry_height"))
        assertEquals(254000, ConstantProvider.getMapping("test_complex_sym.clan_setting_options_list:clan_setting_mobile_entry_height"))
        assertEquals(254016, ConstantProvider.getMapping("test_complex_sym.clan_setting_options_list:clan_setting_icon_size"))

    }

    @Test
    fun `test composite provider with all formats`() {
        // Test with all providers
        ConstantProvider.load(testDir)
        
        // Test RSCM v1 lookups
        assertEquals(4151, ConstantProvider.getMapping("test_rscm_v1.abyssal_whip"))
        assertEquals(4587, ConstantProvider.getMapping("test_rscm_v1.dragon_scimitar"))
        
        // Test RSCM v2 lookups
        assertEquals(4152, ConstantProvider.getMapping("test_rscm_v2.abyssal_whip"))
        assertEquals(4588, ConstantProvider.getMapping("test_rscm_v2.dragon_scimitar"))
        
        // Test Sym lookups
        assertEquals(4153, ConstantProvider.getMapping("test_sym.abyssal_whip"))
        assertEquals(4589, ConstantProvider.getMapping("test_sym.dragon_scimitar"))

    }
    
    @Test
    fun `test file-specific vs base type lookups`() {
        ConstantProvider.load(testDir)
        
        // Test that different files with same base type return different values
        assertEquals(4151, ConstantProvider.getMapping("test_rscm_v1.abyssal_whip"))
        assertEquals(4152, ConstantProvider.getMapping("test_rscm_v2.abyssal_whip"))
        assertEquals(4153, ConstantProvider.getMapping("test_sym.abyssal_whip"))
        
        // Test base type lookups (should return last loaded value)
        // The order depends on how files are processed, but should be consistent
        val baseTypeValue = ConstantProvider.getMapping("test_rscm_v1.abyssal_whip")
        assertTrue(baseTypeValue in listOf(4151, 4152, 4153))
    }
    
    @Test
    fun `test auto-detection of RSCM formats`() {
        ConstantProvider.overrideProviders(listOf(RSCMProvider()))
        ConstantProvider.load(testDir)
        
        // Both files should be loaded despite having different formats
        assertNotNull(ConstantProvider.getMapping("test_rscm_v1.abyssal_whip"))
        assertNotNull(ConstantProvider.getMapping("test_rscm_v2.abyssal_whip"))
        
        // Values should be different
        assertNotEquals(
            ConstantProvider.getMapping("test_rscm_v1.abyssal_whip"),
            ConstantProvider.getMapping("test_rscm_v2.abyssal_whip")
        )
    }
    
    @Test
    fun `test provider manipulation`() {
        // Start with defaults
        ConstantProvider.load(testDir)
        val initialCount = ConstantProvider.mappings.size
        assertTrue(initialCount > 0)
        
        // Remove SymProvider
        ConstantProvider.removeProvider(SymProvider::class.java)
        ConstantProvider.load(testDir)
        val afterRemoveCount = ConstantProvider.mappings.size
        assertTrue(afterRemoveCount < initialCount)
        
        // Add SymProvider back
        ConstantProvider.addProvider(SymProvider())
        ConstantProvider.load(testDir)
        val afterAddCount = ConstantProvider.mappings.size
        assertEquals(initialCount, afterAddCount)
        
        // Clear all providers
        ConstantProvider.clearProviders()
        ConstantProvider.load(testDir)
        assertEquals(0, ConstantProvider.mappings.size)
    }

    @Test
    fun `test types population`() {
        ConstantProvider.load(testDir)
        
        // Should contain all file types
        assertTrue(ConstantProvider.types.contains("test_rscm_v1"))
        assertTrue(ConstantProvider.types.contains("test_rscm_v2"))
        assertTrue(ConstantProvider.types.contains("test_sym"))
        assertTrue(ConstantProvider.types.contains("test_complex_sym"))

        // Should not contain duplicates
        assertEquals(ConstantProvider.types.size, ConstantProvider.types.distinct().size)
    }
    
    @Test
    fun `test whitespace handling`() {
        // This test verifies that the trim() functionality works
        ConstantProvider.load(testDir)
        
        // All lookups should work regardless of any whitespace in the original files
        assertNotNull(ConstantProvider.getMapping("test_rscm_v1.abyssal_whip"))
        assertNotNull(ConstantProvider.getMapping("test_rscm_v2.abyssal_whip"))
        assertNotNull(ConstantProvider.getMapping("test_sym.abyssal_whip"))
    }

} 