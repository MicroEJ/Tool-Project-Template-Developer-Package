# Copyright 2025-2026 MicroEJ Corp. All rights reserved.
# MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.

# Configuration file for the Sphinx documentation builder.
#
# For the full list of built-in configuration values, see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Project information -----------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#project-information
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))

print("Python version:", sys.version)
print("Executable used:", sys.executable)

html_title = full_title = project = "MicroEJ Documentation"
copyright = '2026, MicroEJ Corp. Use is subject to license terms. MicroEJ is a trademark of MicroEJ Corp. All other trademarks and copyrights are the property of their respective owners'
author = 'MicroEJ'
release = '1.0.0'

# -- General configuration ---------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#general-configuration

# Add folder for extensions not packaged in the Gradle plugin
sys.path.append(os.path.abspath('_extensions'))

extensions = [
    'sphinx_design',
    'sphinxcontrib.mermaid',
    'sphinx_tabs.tabs',
]

templates_path = ['_templates']
exclude_patterns = ['sphinx-venv']

# -- Options for HTML output -------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#options-for-html-output

html_theme = 'furo'
html_theme_options = {
    "sidebar_hide_name": True,
    "top_of_page_buttons": [],
    "light_css_variables": {
        "color-sidebar-background-border": "none",
    }
}
html_static_path = ['_static']
html_show_sphinx = False
html_logo = '_templates/img/logo.png'
html_favicon = '_templates/img/favicon.ico'
html_extra_path = ['_icons']

# These paths are either relative to html_static_path
# or fully qualified paths (eg. https://...)
html_css_files = [
    'css/doc.css',
]

# If mermaid_version is set to "",
# the lib won’t be automatically included from the CDN service
# and you’ll need to add it as a local file in html_js_files. For instance,
# if you download the lib to _static/js/mermaid.js, in conf.py:
mermaid_version=""

# Mermaid output format
# Can be "raw", "png" or "svg"
mermaid_output_format="raw"

html_js_files = [
   'js/doc_staticjsmermaid_10.2.0.min.js',
]
