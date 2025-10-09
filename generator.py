#!/usr/bin/env python3
"""
Kotlin Code Snippet Generator using ANTLR4

This module generates random but syntactically correct Kotlin code snippets
by traversing the Kotlin grammar rules.
"""

import random
import string
import argparse
import os
import sys
from typing import Optional, Set

# Add grammar directory to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'grammar'))

from KotlinParser import KotlinParser


class KotlinCodeGenerator:
    """Generates random Kotlin code snippets based on ANTLR4 grammar."""
    
    # Complexity presets
    COMPLEXITY_PRESETS = {
        'low': {'max_depth': 3, 'max_statements': 10, 'branch_probability': 0.3},
        'medium': {'max_depth': 5, 'max_statements': 20, 'branch_probability': 0.5},
        'high': {'max_depth': 8, 'max_statements': 40, 'branch_probability': 0.7},
    }
    
    def __init__(self, max_depth: int = 5, max_statements: int = 20, branch_probability: float = 0.5):
        """
        Initialize the code generator.
        
        Args:
            max_depth: Maximum nesting depth for code structures
            max_statements: Maximum number of statements to generate
            max_branches: Maximum number of branches in control flow
            branch_probability: Probability of including optional elements (0.0 to 1.0)
        """
        self.max_depth = max_depth
        self.max_statements = max_statements
        self.branch_probability = branch_probability
        self.current_depth = 0
        self.statement_count = 0
        self.used_identifiers: Set[str] = set()
        self.indent_level = 0
        
    @classmethod
    def from_complexity(cls, complexity: str) -> 'KotlinCodeGenerator':
        """Create a generator from a complexity preset."""
        if complexity not in cls.COMPLEXITY_PRESETS:
            raise ValueError(f"Unknown complexity: {complexity}. Choose from {list(cls.COMPLEXITY_PRESETS.keys())}")
        config = cls.COMPLEXITY_PRESETS[complexity]
        return cls(**config)
    
    def generate_identifier(self, prefix: str = "id") -> str:
        """Generate a unique identifier."""
        base = f"{prefix}{len(self.used_identifiers)}"
        while base in self.used_identifiers:
            base = f"{prefix}{random.randint(0, 9999)}"
        self.used_identifiers.add(base)
        return base
    
    def generate_simple_type(self) -> str:
        """Generate a simple type name."""
        types = ['Int', 'String', 'Boolean', 'Double', 'Float', 'Long', 'Char']
        return random.choice(types)
    
    def generate_literal(self, type_name: str = None) -> str:
        """Generate a literal value."""
        if type_name == 'Int' or type_name is None:
            return str(random.randint(0, 100))
        elif type_name == 'String':
            return f'"{self.generate_identifier("str")}"'
        elif type_name == 'Boolean':
            return random.choice(['true', 'false'])
        elif type_name == 'Double':
            return f"{random.uniform(0, 100):.2f}"
        elif type_name == 'Float':
            return f"{random.uniform(0, 100):.2f}f"
        elif type_name == 'Long':
            return f"{random.randint(0, 100)}L"
        elif type_name == 'Char':
            return f"'{random.choice(string.ascii_lowercase)}'"
        return str(random.randint(0, 100))
    
    def indent(self) -> str:
        """Return the current indentation string."""
        return "    " * self.indent_level
    
    def generate_function(self) -> str:
        """Generate a simple function."""
        if self.current_depth >= self.max_depth:
            return ""
        
        self.current_depth += 1
        func_name = self.generate_identifier("func")
        return_type = self.generate_simple_type() if random.random() > 0.3 else "Unit"
        
        # Generate parameters
        param_count = random.randint(0, 3)
        params = []
        for _ in range(param_count):
            param_name = self.generate_identifier("param")
            param_type = self.generate_simple_type()
            params.append(f"{param_name}: {param_type}")
        
        params_str = ", ".join(params)
        
        lines = [f"{self.indent()}fun {func_name}({params_str}): {return_type} {{"]
        self.indent_level += 1
        
        # Generate function body
        body = self.generate_statements(max_count=min(5, self.max_statements))
        lines.extend(body)
        
        # Add return statement if needed
        if return_type != "Unit" and (not body or "return" not in body[-1]):
            lines.append(f"{self.indent()}return {self.generate_literal(return_type)}")
        
        self.indent_level -= 1
        lines.append(f"{self.indent()}}}")
        
        self.current_depth -= 1
        return "\n".join(lines)
    
    def generate_class(self) -> str:
        """Generate a simple class."""
        if self.current_depth >= self.max_depth:
            return ""
        
        self.current_depth += 1
        class_name = self.generate_identifier("Class").capitalize()
        
        lines = [f"{self.indent()}class {class_name} {{"]
        self.indent_level += 1
        
        # Generate properties
        prop_count = random.randint(1, 3)
        for _ in range(prop_count):
            prop_name = self.generate_identifier("prop")
            prop_type = self.generate_simple_type()
            mutability = random.choice(['val', 'var'])
            lines.append(f"{self.indent()}{mutability} {prop_name}: {prop_type} = {self.generate_literal(prop_type)}")
        
        # Maybe add a method
        if random.random() > 0.5 and self.current_depth < self.max_depth:
            lines.append("")
            lines.append(self.generate_function())
        
        self.indent_level -= 1
        lines.append(f"{self.indent()}}}")
        
        self.current_depth -= 1
        return "\n".join(lines)
    
    def generate_if_statement(self) -> str:
        """Generate an if statement."""
        if self.current_depth >= self.max_depth:
            return ""
        
        self.current_depth += 1
        
        # Generate condition
        var1 = self.generate_identifier("var")
        var2 = self.generate_literal('Int')
        operator = random.choice(['>', '<', '==', '!=', '>=', '<='])
        condition = f"{var1} {operator} {var2}"
        
        lines = [f"{self.indent()}if ({condition}) {{"]
        self.indent_level += 1
        lines.extend(self.generate_statements(max_count=3))
        self.indent_level -= 1
        lines.append(f"{self.indent()}}}")
        
        # Maybe add else
        if random.random() > 0.5:
            lines.append(f"{self.indent()}else {{")
            self.indent_level += 1
            lines.extend(self.generate_statements(max_count=2))
            self.indent_level -= 1
            lines.append(f"{self.indent()}}}")
        
        self.current_depth -= 1
        return "\n".join(lines)
    
    def generate_for_loop(self) -> str:
        """Generate a for loop."""
        if self.current_depth >= self.max_depth:
            return ""
        
        self.current_depth += 1
        
        loop_var = self.generate_identifier("i")
        start = random.randint(0, 5)
        end = random.randint(start + 1, start + 10)
        
        lines = [f"{self.indent()}for ({loop_var} in {start}..{end}) {{"]
        self.indent_level += 1
        lines.extend(self.generate_statements(max_count=3))
        self.indent_level -= 1
        lines.append(f"{self.indent()}}}")
        
        self.current_depth -= 1
        return "\n".join(lines)
    
    def generate_while_loop(self) -> str:
        """Generate a while loop."""
        if self.current_depth >= self.max_depth:
            return ""
        
        self.current_depth += 1
        
        var = self.generate_identifier("count")
        limit = random.randint(5, 10)
        
        lines = [
            f"{self.indent()}var {var} = 0",
            f"{self.indent()}while ({var} < {limit}) {{"
        ]
        self.indent_level += 1
        lines.extend(self.generate_statements(max_count=2))
        lines.append(f"{self.indent()}{var}++")
        self.indent_level -= 1
        lines.append(f"{self.indent()}}}")
        
        self.current_depth -= 1
        return "\n".join(lines)
    
    def generate_variable_declaration(self) -> str:
        """Generate a variable declaration."""
        var_name = self.generate_identifier("var")
        var_type = self.generate_simple_type()
        mutability = random.choice(['val', 'var'])
        value = self.generate_literal(var_type)
        return f"{self.indent()}{mutability} {var_name}: {var_type} = {value}"
    
    def generate_print_statement(self) -> str:
        """Generate a println statement."""
        if self.used_identifiers and random.random() > 0.5:
            var = random.choice(list(self.used_identifiers))
            return f'{self.indent()}println({var})'
        else:
            return f'{self.indent()}println({self.generate_literal("String")})'
    
    def generate_statement(self) -> str:
        """Generate a single statement."""
        if self.statement_count >= self.max_statements:
            return ""
        
        self.statement_count += 1
        
        # Weight simpler statements higher at deeper levels
        depth_factor = self.current_depth / max(self.max_depth, 1)
        
        choices = [
            (self.generate_variable_declaration, 3.0 - depth_factor),
            (self.generate_print_statement, 2.0),
        ]
        
        # Add complex statements only if not too deep
        if self.current_depth < self.max_depth:
            choices.extend([
                (self.generate_if_statement, 1.5 * (1 - depth_factor)),
                (self.generate_for_loop, 1.0 * (1 - depth_factor)),
                (self.generate_while_loop, 0.8 * (1 - depth_factor)),
            ])
        
        # Weighted random choice
        functions, weights = zip(*choices)
        func = random.choices(functions, weights=weights)[0]
        return func()
    
    def generate_statements(self, max_count: int = None) -> list:
        """Generate multiple statements."""
        if max_count is None:
            max_count = self.max_statements
        
        # Ensure we don't exceed statement limit
        remaining = self.max_statements - self.statement_count
        if remaining <= 0:
            return []
        
        # Generate between 1 and min(max_count, remaining) statements
        max_to_generate = min(max_count, remaining)
        count = random.randint(1, max(1, max_to_generate))
        statements = []
        
        for _ in range(count):
            if self.statement_count >= self.max_statements:
                break
            stmt = self.generate_statement()
            if stmt:
                statements.append(stmt)
        
        return statements
    
    def generate_main_function(self) -> str:
        """Generate a main function with random content."""
        lines = ["fun main() {"]
        self.indent_level += 1
        
        # Generate main body
        lines.extend(self.generate_statements())
        
        self.indent_level -= 1
        lines.append("}")
        
        return "\n".join(lines)
    
    def generate_top_level_code(self) -> str:
        """Generate top-level Kotlin code."""
        self.current_depth = 0
        self.statement_count = 0
        self.used_identifiers.clear()
        self.indent_level = 0
        
        lines = []
        
        # Decide what to generate
        include_class = random.random() > 0.5 and self.max_depth > 2
        include_function = random.random() > 0.3 and self.max_depth > 1
        include_main = random.random() > 0.5
        
        # Generate class
        if include_class:
            lines.append(self.generate_class())
            lines.append("")
        
        # Generate standalone function
        if include_function:
            lines.append(self.generate_function())
            lines.append("")
        
        # Generate main function
        if include_main or (not include_class and not include_function):
            lines.append(self.generate_main_function())
        
        return "\n".join(lines)
    
    def generate(self) -> str:
        """Generate a complete Kotlin code snippet."""
        return self.generate_top_level_code()


def main():
    """Main entry point for the generator."""
    parser = argparse.ArgumentParser(
        description='Generate random Kotlin code snippets using ANTLR4 grammar',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s                                  # Generate one snippet
  %(prog)s --count 10                       # Generate 10 snippets
  %(prog)s --complexity low                 # Generate low complexity code
  %(prog)s --max-depth 6 --max-statements 25  # Custom complexity
  %(prog)s --count 100 --output-dir ./samples  # Save to files
        """
    )
    
    parser.add_argument(
        '--count', '-c',
        type=int,
        default=1,
        help='Number of code snippets to generate (default: 1)'
    )
    
    parser.add_argument(
        '--complexity',
        choices=['low', 'medium', 'high'],
        help='Complexity preset (overrides max-depth and max-statements)'
    )
    
    parser.add_argument(
        '--max-depth',
        type=int,
        default=5,
        help='Maximum nesting depth (default: 5)'
    )
    
    parser.add_argument(
        '--max-statements',
        type=int,
        default=20,
        help='Maximum number of statements (default: 20)'
    )
    
    parser.add_argument(
        '--branch-probability',
        type=float,
        default=0.5,
        help='Probability of including optional elements (0.0-1.0, default: 0.5)'
    )
    
    parser.add_argument(
        '--output-dir', '-o',
        type=str,
        help='Directory to save generated files (default: print to stdout)'
    )
    
    parser.add_argument(
        '--seed',
        type=int,
        help='Random seed for reproducibility'
    )
    
    args = parser.parse_args()
    
    # Set random seed if provided
    if args.seed is not None:
        random.seed(args.seed)
    
    # Create generator
    if args.complexity:
        generator_class = KotlinCodeGenerator.from_complexity(args.complexity)
    else:
        generator_class = KotlinCodeGenerator(
            max_depth=args.max_depth,
            max_statements=args.max_statements,
            branch_probability=args.branch_probability
        )
    
    # Create output directory if needed
    if args.output_dir:
        os.makedirs(args.output_dir, exist_ok=True)
    
    # Generate code snippets
    for i in range(args.count):
        # Create a fresh generator for each snippet
        if args.complexity:
            generator = KotlinCodeGenerator.from_complexity(args.complexity)
        else:
            generator = KotlinCodeGenerator(
                max_depth=args.max_depth,
                max_statements=args.max_statements,
                branch_probability=args.branch_probability
            )
        
        code = generator.generate()
        
        if args.output_dir:
            filename = os.path.join(args.output_dir, f"sample_{i:04d}.kt")
            with open(filename, 'w') as f:
                f.write(code)
            print(f"Generated: {filename}")
        else:
            if args.count > 1:
                print(f"\n{'='*60}")
                print(f"Snippet {i+1}/{args.count}")
                print('='*60)
            print(code)


if __name__ == '__main__':
    main()
