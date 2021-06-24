// Copyright 2011 The Whiley Project Developers
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
/**
 * <p>
 * <b>The Whiley File System</b>. This provides a generic and flexible
 * representation of hierarchically named objects. In essence, this is an
 * abstract view of a "filesystem" which may be mapped to a physical file system
 * (e.g. files, directories) or something else (e.g. an in-memory file system,
 * the Eclipse filesystem, etc).
 * </p>
 * <p>
 * A standard implementation of the file system is also provided which is based
 * around physical files, directories and archive files (e.g. jars). This
 * standard implementation is sufficient for a command-line compiler. However,
 * when running a compiler from within an IDE (e.g. Eclipse) alternative
 * implementations specific to the IDE may be required.
 * </p>
 *
 * @author David J. Pearce
 */
package wyfs;

