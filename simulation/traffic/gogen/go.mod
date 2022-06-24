module chandler.com/gogen

go 1.14

require (
	github.com/coreos/etcd v3.3.25+incompatible // indirect
	github.com/coreos/go-semver v0.3.0 // indirect
	github.com/coreos/go-systemd v0.0.0-20191104093116-d3cd4ed1dbcf // indirect
	github.com/coreos/pkg v0.0.0-20180928190104-399ea9e2e55f // indirect
	github.com/fsnotify/fsnotify v1.4.9
	github.com/gin-gonic/gin v1.6.3
	github.com/go-playground/validator/v10 v10.3.0 // indirect
	github.com/go-redis/redis/v8 v8.0.0-beta.12
	github.com/gogo/protobuf v1.3.1 // indirect
	github.com/google/gopacket v1.1.19
	github.com/google/uuid v1.1.2 // indirect
	github.com/gorilla/websocket v1.4.2 // indirect
	github.com/json-iterator/go v1.1.10 // indirect
	github.com/mattn/go-sqlite3 v1.14.3
	github.com/modern-go/concurrent v0.0.0-20180306012644-bacd9c7ef1dd // indirect
	github.com/modern-go/reflect2 v1.0.1 // indirect
	go.etcd.io/etcd v3.3.25+incompatible // indirect
	go.mongodb.org/mongo-driver v1.4.1
	go.uber.org/zap v1.16.0 // indirect
	golang.org/x/sys v0.0.0-20200909081042-eff7692f9009 // indirect
	gonum.org/v1/gonum v0.7.0
	google.golang.org/protobuf v1.25.0 // indirect
	gorm.io/driver/sqlite v1.1.2 // indirect
	gorm.io/gorm v1.20.1 // indirect
)

replace chandler.com/gogen/gen => ./gen

replace chandler.com/gogen/utils => ./utils
