#!/usr/bin/env python3
"""
C++ Header Parser - Extract function names and count from C++ header files

This script parses a C++ header file and extracts:
- Total number of functions
- List of function names

It uses regex patterns to identify function declarations while ignoring:
- Macros
- Comments
- Preprocessor directives
"""

import re
import sys
from typing import List, Tuple


def remove_comments(content: str) -> str:
    """
    Remove C++ style comments from the content.
    
    Args:
        content: The source code content
        
    Returns:
        Content with comments removed
    """
    # Remove single-line comments
    content = re.sub(r'//.*?$', '', content, flags=re.MULTILINE)
    # Remove multi-line comments
    content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)
    return content


def remove_macros(content: str) -> str:
    """
    Remove preprocessor directives and macros.
    
    Args:
        content: The source code content
        
    Returns:
        Content with macros removed
    """
    # Remove lines starting with #
    content = re.sub(r'^\s*#.*?$', '', content, flags=re.MULTILINE)
    return content


def extract_functions(content: str) -> List[str]:
    """
    Extract function names from C++ header content.
    
    This function looks for patterns like:
    - return_type function_name(parameters);
    - return_type function_name(parameters) const;
    - virtual return_type function_name(parameters);
    - static return_type function_name(parameters);
    
    Args:
        content: The cleaned source code content
        
    Returns:
        List of function names found
    """
    functions = []
    
    # First, remove template declarations (template<...> lines)
    # to avoid matching type parameters as functions
    content = re.sub(r'^\s*template\s*<[^>]*>\s*$', '', content, flags=re.MULTILINE)
    
    # Pattern to match function declarations
    # This pattern matches: [modifiers] return_type function_name(params) [const] [=0];
    # We capture the function name
    pattern = r'''
        (?:virtual\s+|static\s+|inline\s+|explicit\s+)*  # Optional modifiers
        (?:[\w:]+(?:<[\w\s,]+>)?\s*[\*&]?\s+)+           # Return type (can include namespace, templates, pointers, references)
        (\w+)                                             # Function name (captured)
        \s*\(                                             # Opening parenthesis
        [^)]*                                             # Parameters (any characters except closing paren)
        \)                                                # Closing parenthesis
        (?:\s*const)?                                     # Optional const qualifier
        (?:\s*=\s*0)?                                     # Optional pure virtual
        (?:\s*=\s*default)?                               # Optional = default
        (?:\s*=\s*delete)?                                # Optional = delete
        \s*;                                              # Semicolon
    '''
    
    matches = re.finditer(pattern, content, re.VERBOSE | re.MULTILINE)
    
    for match in matches:
        function_name = match.group(1)
        # Exclude common keywords that might be mistakenly matched
        if function_name not in ['if', 'while', 'for', 'switch', 'return']:
            functions.append(function_name)
    
    return functions


def parse_cpp_header(file_path: str) -> Tuple[int, List[str]]:
    """
    Parse a C++ header file and extract function information.
    
    Args:
        file_path: Path to the C++ header file
        
    Returns:
        Tuple of (function_count, function_names)
    """
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
    except FileNotFoundError:
        print(f"Error: File '{file_path}' not found.")
        sys.exit(1)
    except Exception as e:
        print(f"Error reading file: {e}")
        sys.exit(1)
    
    # Clean the content
    content = remove_comments(content)
    content = remove_macros(content)
    
    # Extract functions
    functions = extract_functions(content)
    
    return len(functions), functions


def main():
    """Main entry point for the script."""
    if len(sys.argv) != 2:
        print("Usage: python cpp_header_parser.py <header_file.h>")
        print("\nExample:")
        print("  python cpp_header_parser.py sample.h")
        sys.exit(1)
    
    header_file = sys.argv[1]
    
    print(f"Parsing C++ header file: {header_file}")
    print("=" * 60)
    
    # Parse the header
    func_count, func_names = parse_cpp_header(header_file)
    
    # Display results
    print(f"\nTotal number of functions: {func_count}")
    print("\nFunction names:")
    for i, func_name in enumerate(func_names, 1):
        print(f"  {i}. {func_name}")
    
    print("\n" + "=" * 60)
    print("Parsing complete!")


if __name__ == "__main__":
    main()
