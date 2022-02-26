FROM mcr.microsoft.com/dotnet/runtime:6.0 AS base
WORKDIR /app

FROM mcr.microsoft.com/dotnet/sdk:6.0 AS build
WORKDIR /src
COPY ["metrics-service/metrics-service.csproj", "metrics-service/"]
RUN dotnet restore "metrics-service/metrics-service.csproj"
COPY . .
WORKDIR "/src/metrics-service"
RUN dotnet build "metrics-service.csproj" -c Release -o /app/build

FROM build AS publish
RUN dotnet publish "metrics-service.csproj" -c Release -o /app/publish

FROM base AS final
WORKDIR /app
COPY --from=publish /app/publish .
ENTRYPOINT ["dotnet", "metrics-service.dll"]
