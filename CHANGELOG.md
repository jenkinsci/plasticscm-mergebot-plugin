# Release notes

## Deprecation (02 Jan 2024)

This plugin is no longer relevant.

## Version 0.16 (21 Dec 2022)

* Support object specs for cloud-based repos.

## Version 0.15 (24 May 2021)

* Upgrade the XML parser dependency package.

## Version 0.14 (22 Oct 2019)

### Fixes

* The checkout operation failed if the workspace path contained whitespace. Fixed!

## Version 0.13 (12 Jun 2019)

### Fixes

* The Jenkins job using mergebot sometimes failed showing the error message
  "Workspace already exists".
  * This happened when the Jenkins server ran in Linux but job agents ran in Windows.
  * The error appeared when the casing of the root directory path drive letter in the Windows agent
  didn't match the workspace path.
    * E.g. workspace path was `c:\jenkins\plan1` but root directory was `C:\jenkins`

## Version 0.12 (08 Oct 2018)

### Fixes

* We improved how the plugin reports errors in Pipelines that use lightweight checkout. Before this,
  error reports only contained `NULL`. The plugin now displays the complete command execution.
* Fixed an incompatibility with other plugins if they require the SCM plugin to support the
  `ChangeLogSet.Entry.getAffectedFiles()` method.

## Version 0.11 (28 Sept 2018)

### Fixes

* Added pipeline support, including lightweight checkout support.
* Ensured environment variables are available for build steps in all platforms.
* **Warning:** Make sure the installed client version in your Jenkins machines is **7.0.16.2630** or
  higher.

## Version 0.10 (06 Sept 2018)

### Fixes

* Initial preview version. Supports only freestyle projects.
