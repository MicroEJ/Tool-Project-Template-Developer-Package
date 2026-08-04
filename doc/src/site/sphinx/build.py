# Copyright 2025-2026 MicroEJ Corp. All rights reserved.
# MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
# Build: 7E4D1F7C

import os
import sys
import shutil
import subprocess

print("Using Python version:", sys.version)
print("Python executable:", sys.executable)

# Define paths
doc_output_folder = sys.argv[1] if len(sys.argv) >= 2 else "doc_build"
pages_folder = "pages"
source_folder = "."
pip_command = [sys.executable, "-m", "pip", "install", "-r", "requirements.txt"]
build_command = [sys.executable, "-m", "sphinx", "-W", "-b", "html", source_folder, doc_output_folder]

def clean_folder(folder_path):
    """Delete the specified folder and its contents."""
    if os.path.exists(folder_path):
        print(f"Cleaning folder: {folder_path}")
        try:
            shutil.rmtree(folder_path)
        except OSError:
            # On Windows, the directory itself may be locked by the OS (e.g. Indexer, Defender).
            # Clear its contents instead so Sphinx can rebuild into the empty directory.
            for entry in os.scandir(folder_path):
                if entry.is_dir(follow_symlinks=False):
                    shutil.rmtree(entry.path)
                else:
                    os.remove(entry.path)
    else:
        print(f"Folder does not exist, skipping clean: {folder_path}")

def install_packages():
    """Install the required Python packages."""
    try:
        print("Installing packages...")
        subprocess.run(pip_command, check=True)
        print("Packages installed successfully.")
    except subprocess.CalledProcessError as e:
        print(f"Error during Sphinx build: {e}")
        exit(1)

def build_docs():
    """Build the Sphinx documentation."""
    try:
        print("Starting Sphinx build...")
        subprocess.run(build_command, check=True)
        print("Sphinx build completed successfully.")
    except subprocess.CalledProcessError as e:
        print(f"Error during Sphinx build: {e}")
        exit(1)

'''
def annotate_images():
    annotate_command = [sys.executable,"./documentation_tools/board_annotate/doc_annotate.py", "-a","./documentation_tools/board_annotate/board_annotate.py", "-i", source_folder+"/"+pages_folder,"-o", source_folder+"/"+doc_output_folder+"/_images","-f","./documentation_tools/board_annotate/SourceSansPro-Italic.ttf"]
    subprocess.run(annotate_command,check=True)
'''

if __name__ == "__main__":
    clean_folder(doc_output_folder)
    install_packages()
    build_docs()
    #annotate_images()