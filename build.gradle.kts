tasks {
	val wrapper by existing(Wrapper::class) {
		distributionType = Wrapper.DistributionType.ALL
		gradleVersion = "5.4.1"
	}
}
