using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using NetLoanCalculator.Services;
using Steeltoe.Extensions.Configuration.CloudFoundry;
using Steeltoe.Management.CloudFoundry;
using Swashbuckle.AspNetCore.Swagger;
using Steeltoe.CloudFoundry.Connector.Redis;

namespace NetLoanCalculatorRedis
{
    public class Startup
    {
        public Startup(IConfiguration configuration, IHostingEnvironment environment)
        {
            Configuration = configuration;
            env = environment;
        }

        public IConfiguration Configuration { get; }
        public IHostingEnvironment env {get; }

        // This method gets called by the runtime. Use this method to add services to the container.
        public void ConfigureServices(IServiceCollection services)
        {
            services.AddCors();
            services.AddMvc().SetCompatibilityVersion(CompatibilityVersion.Version_2_1);

            services.AddOptions();
            services.ConfigureCloudFoundryOptions(Configuration);

            if (env.IsDevelopment())
            {
                services.AddSingleton<IHitCountService, MemoryHitCountService>();
            }
            else
            {
                services.AddRedisConnectionMultiplexer(Configuration);
                services.AddSingleton<IHitCountService, RedisHitCountService>();
            }

            services.AddSingleton<PaymentCalculator>(new PaymentCalculator());
            services.AddSingleton<Crasher>(new Crasher());

            services.AddSwaggerGen(c =>
            {
                c.SwaggerDoc("v1", new Info { Title = "My API", Version = "v1" });
            });

            services.AddCloudFoundryActuators(Configuration);
        }

        // This method gets called by the runtime. Use this method to configure the HTTP request pipeline.
        public void Configure(IApplicationBuilder app, IHostingEnvironment env)
        {
            if (env.IsDevelopment())
            {
                app.UseDeveloperExceptionPage();
            }
            else
            {
                app.UseHsts();
                // bug in Steeltoe 2.2.0 - this will cause a crash when locally, so only run
                // when deployed to CF
                // possibly resolved in https://github.com/SteeltoeOSS/Management/pull/53
                app.UseCloudFoundryActuators();
            }

            app.UseCors(builder => builder.AllowAnyOrigin());
            app.UseHttpsRedirection();

            app.UseSwagger();
            app.UseSwaggerUI(c =>
            {
                c.SwaggerEndpoint("/swagger/v1/swagger.json", "My API V1");
                c.RoutePrefix = string.Empty;
            });

            app.UseMvc();
        }
    }
}
