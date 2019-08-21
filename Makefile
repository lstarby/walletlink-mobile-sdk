format:
	ios/Pods/SwiftFormat/CommandLineTool/swiftformat ios/Source --exclude Toshi/Generated/Code --header "// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>\n// Licensed under the Apache License, version 2.0"
	android/gradlew ktlintFormat -p android

lint:
	ios/Pods/SwiftLint/swiftlint --path ios
	android/gradlew ktlint -p android

deps:
	rm -rf libraries; git submodule update --init --force --recursive
