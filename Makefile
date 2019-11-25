format:
	ios/Pods/SwiftFormat/CommandLineTool/swiftformat ios/Source --exclude Toshi/Generated/Code --header "// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>\n// Licensed under the Apache License, version 2.0"
	sh set_copyright_headers.sh
	android/gradlew ktlintFormat -p android

lint:
	ios/Pods/SwiftLint/swiftlint --path ios
	android/gradlew ktlint -p android

deps:
	rm -rf libraries; git submodule update --init --force --recursive
ifdef update
	# Pull latest submodule version for each submodule
	git submodule foreach 'git checkout master && git reset --hard origin/master && git pull || :'
else
  	# Pull pinned submodule version for each submodule
	git submodule foreach 'git checkout $$sha1 || :'
endif
