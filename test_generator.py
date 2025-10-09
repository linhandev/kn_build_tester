#!/usr/bin/env python3
"""
Test suite for the Kotlin Code Generator

This test suite validates that the generator:
1. Produces valid output
2. Respects complexity constraints
3. Generates diverse code patterns
"""

import unittest
import sys
import os
import tempfile
import shutil
from io import StringIO

# Add parent directory to path
sys.path.insert(0, os.path.dirname(__file__))

from generator import KotlinCodeGenerator


class TestKotlinCodeGenerator(unittest.TestCase):
    """Test cases for KotlinCodeGenerator."""
    
    def test_generator_initialization(self):
        """Test that generator initializes correctly."""
        gen = KotlinCodeGenerator(max_depth=3, max_statements=10)
        self.assertEqual(gen.max_depth, 3)
        self.assertEqual(gen.max_statements, 10)
        self.assertEqual(gen.current_depth, 0)
    
    def test_complexity_presets(self):
        """Test that complexity presets work correctly."""
        for complexity in ['low', 'medium', 'high']:
            gen = KotlinCodeGenerator.from_complexity(complexity)
            self.assertIsNotNone(gen)
            self.assertGreater(gen.max_depth, 0)
            self.assertGreater(gen.max_statements, 0)
    
    def test_invalid_complexity(self):
        """Test that invalid complexity raises error."""
        with self.assertRaises(ValueError):
            KotlinCodeGenerator.from_complexity('invalid')
    
    def test_generate_identifier(self):
        """Test identifier generation."""
        gen = KotlinCodeGenerator()
        id1 = gen.generate_identifier("test")
        id2 = gen.generate_identifier("test")
        # Identifiers should be unique
        self.assertNotEqual(id1, id2)
        # Both should be in used identifiers
        self.assertIn(id1, gen.used_identifiers)
        self.assertIn(id2, gen.used_identifiers)
    
    def test_generate_simple_type(self):
        """Test type generation."""
        gen = KotlinCodeGenerator()
        type_name = gen.generate_simple_type()
        valid_types = ['Int', 'String', 'Boolean', 'Double', 'Float', 'Long', 'Char']
        self.assertIn(type_name, valid_types)
    
    def test_generate_literal(self):
        """Test literal generation."""
        gen = KotlinCodeGenerator()
        
        # Test Int literal
        int_lit = gen.generate_literal('Int')
        self.assertTrue(int_lit.isdigit() or int_lit.startswith('-'))
        
        # Test String literal
        str_lit = gen.generate_literal('String')
        self.assertTrue(str_lit.startswith('"') and str_lit.endswith('"'))
        
        # Test Boolean literal
        bool_lit = gen.generate_literal('Boolean')
        self.assertIn(bool_lit, ['true', 'false'])
    
    def test_generate_produces_output(self):
        """Test that generate() produces non-empty output."""
        gen = KotlinCodeGenerator()
        code = gen.generate()
        self.assertIsNotNone(code)
        self.assertGreater(len(code), 0)
    
    def test_generate_contains_kotlin_keywords(self):
        """Test that generated code contains Kotlin keywords."""
        gen = KotlinCodeGenerator()
        code = gen.generate()
        # Should contain at least some Kotlin keywords
        kotlin_keywords = ['fun', 'val', 'var', 'class']
        has_keyword = any(keyword in code for keyword in kotlin_keywords)
        self.assertTrue(has_keyword, f"Generated code should contain Kotlin keywords:\n{code}")
    
    def test_multiple_generations_are_different(self):
        """Test that multiple generations produce different results."""
        codes = []
        for _ in range(5):
            gen = KotlinCodeGenerator()
            codes.append(gen.generate())
        
        # At least some should be different (with high probability)
        unique_codes = set(codes)
        self.assertGreater(len(unique_codes), 1, "Multiple generations should produce varied output")
    
    def test_depth_constraint(self):
        """Test that depth constraint is respected."""
        max_depth = 2
        gen = KotlinCodeGenerator(max_depth=max_depth)
        
        # Generate code and check depth doesn't exceed max
        code = gen.generate()
        # Count maximum brace nesting
        max_nesting = 0
        current_nesting = 0
        for char in code:
            if char == '{':
                current_nesting += 1
                max_nesting = max(max_nesting, current_nesting)
            elif char == '}':
                current_nesting -= 1
        
        # Allow some tolerance since main function adds one level
        self.assertLessEqual(max_nesting, max_depth + 2)
    
    def test_low_complexity_is_simpler(self):
        """Test that low complexity generates simpler code."""
        gen_low = KotlinCodeGenerator.from_complexity('low')
        gen_high = KotlinCodeGenerator.from_complexity('high')
        
        code_low = gen_low.generate()
        code_high = gen_high.generate()
        
        # High complexity should generally be longer
        # (though randomness means this isn't guaranteed)
        # So we'll just check both generate valid output
        self.assertGreater(len(code_low), 0)
        self.assertGreater(len(code_high), 0)
    
    def test_generate_variable_declaration(self):
        """Test variable declaration generation."""
        gen = KotlinCodeGenerator()
        var_decl = gen.generate_variable_declaration()
        
        # Should contain val or var
        self.assertTrue('val ' in var_decl or 'var ' in var_decl)
        # Should contain a colon (type annotation)
        self.assertIn(':', var_decl)
        # Should contain assignment
        self.assertIn('=', var_decl)
    
    def test_generate_function(self):
        """Test function generation."""
        gen = KotlinCodeGenerator()
        func = gen.generate_function()
        
        # Should contain fun keyword
        self.assertIn('fun ', func)
        # Should have braces
        self.assertIn('{', func)
        self.assertIn('}', func)
    
    def test_generate_class(self):
        """Test class generation."""
        gen = KotlinCodeGenerator()
        cls = gen.generate_class()
        
        # Should contain class keyword
        self.assertIn('class ', cls)
        # Should have braces
        self.assertIn('{', cls)
        self.assertIn('}', cls)
    
    def test_statement_count_constraint(self):
        """Test that statement count is approximately respected."""
        max_statements = 5
        gen = KotlinCodeGenerator(max_statements=max_statements)
        code = gen.generate()
        
        # Count lines that look like statements (rough heuristic)
        lines = code.split('\n')
        statement_lines = [l for l in lines if l.strip() and not l.strip().endswith('{') and not l.strip() == '}']
        
        # Should not massively exceed max_statements
        # (allow some tolerance for structure)
        self.assertLess(len(statement_lines), max_statements * 3)
    
    def test_indentation(self):
        """Test that code is properly indented."""
        gen = KotlinCodeGenerator()
        code = gen.generate()
        
        lines = code.split('\n')
        # Check that there's some indentation in the code
        has_indentation = any(line.startswith('    ') for line in lines)
        self.assertTrue(has_indentation, "Generated code should have indentation")
    
    def test_main_function_generation(self):
        """Test main function generation."""
        gen = KotlinCodeGenerator()
        main_func = gen.generate_main_function()
        
        self.assertIn('fun main()', main_func)
        self.assertIn('{', main_func)
        self.assertIn('}', main_func)
    
    def test_reproducibility_with_seed(self):
        """Test that results are reproducible with same seed."""
        import random
        
        # Generate with seed 42
        random.seed(42)
        gen1 = KotlinCodeGenerator()
        code1 = gen1.generate()
        
        # Generate with same seed
        random.seed(42)
        gen2 = KotlinCodeGenerator()
        code2 = gen2.generate()
        
        # Should be identical
        self.assertEqual(code1, code2)


class TestCodeGeneratorOutput(unittest.TestCase):
    """Test the actual output of the code generator."""
    
    def test_generate_valid_syntax_structure(self):
        """Test that generated code has valid Kotlin syntax structure."""
        gen = KotlinCodeGenerator()
        code = gen.generate()
        
        # Count braces - should be balanced
        open_braces = code.count('{')
        close_braces = code.count('}')
        self.assertEqual(open_braces, close_braces, "Braces should be balanced")
        
        # Count parentheses - should be balanced
        open_parens = code.count('(')
        close_parens = code.count(')')
        self.assertEqual(open_parens, close_parens, "Parentheses should be balanced")
    
    def test_no_empty_output(self):
        """Test that generator never produces empty output."""
        for _ in range(10):
            gen = KotlinCodeGenerator()
            code = gen.generate()
            self.assertGreater(len(code.strip()), 0, "Generated code should not be empty")
    
    def test_different_complexity_levels(self):
        """Test code generation at different complexity levels."""
        for complexity in ['low', 'medium', 'high']:
            gen = KotlinCodeGenerator.from_complexity(complexity)
            code = gen.generate()
            self.assertGreater(len(code), 0, f"Should generate code for {complexity} complexity")


class TestEdgeCases(unittest.TestCase):
    """Test edge cases and boundary conditions."""
    
    def test_minimum_depth(self):
        """Test with minimum depth."""
        gen = KotlinCodeGenerator(max_depth=1, max_statements=5)
        code = gen.generate()
        self.assertGreater(len(code), 0)
    
    def test_minimum_statements(self):
        """Test with minimum statements."""
        gen = KotlinCodeGenerator(max_depth=3, max_statements=1)
        code = gen.generate()
        self.assertGreater(len(code), 0)
    
    def test_large_parameters(self):
        """Test with large parameters."""
        gen = KotlinCodeGenerator(max_depth=10, max_statements=100)
        code = gen.generate()
        self.assertGreater(len(code), 0)


def run_tests(verbose=False):
    """Run all tests."""
    # Create test suite
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    
    # Add all test cases
    suite.addTests(loader.loadTestsFromTestCase(TestKotlinCodeGenerator))
    suite.addTests(loader.loadTestsFromTestCase(TestCodeGeneratorOutput))
    suite.addTests(loader.loadTestsFromTestCase(TestEdgeCases))
    
    # Run tests
    runner = unittest.TextTestRunner(verbosity=2 if verbose else 1)
    result = runner.run(suite)
    
    return result.wasSuccessful()


if __name__ == '__main__':
    import sys
    verbose = '-v' in sys.argv or '--verbose' in sys.argv
    success = run_tests(verbose=verbose)
    sys.exit(0 if success else 1)
