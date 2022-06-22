COMPILE_DEPS = CORE_DEPS + JACKSON + REST + KRYO + CLI + [
    "//core/common:onos-core-common",
    "@minimal_json//:minimal_json",
    #    "@maven//:redis_clients_jedis",
    #    "@jedis//:jedis",
]

osgi_jar_with_tests(
    suppress_checkstyle = True,
    deps = COMPILE_DEPS,
)

#REQUIRE_APPS = [
#    #    "org.onosproject.proxyarp",
#    "com.eclipsesource.json.JsonArray",
#]

onos_app(
    category = "Self defined onos",
    description = "Project smartfwd",
    #    required_apps = REQUIRE_APPS,
    title = "SmartFWD",
    url = "http://onosproject.org",
)
